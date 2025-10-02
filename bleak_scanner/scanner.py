# scanner.py
import asyncio, struct
from bleak import BleakScanner

import numpy as np

TARGET_SERVICE_UUID = "0000feed-0000-1000-8000-00805f9b34fb"
DEVICE_NAME_PREFIX = "IndoorScanner"
PATH_LOSS_N = 2.7
N = 256

tx_power_matrix = [0] * N
seen_students = set()

# Initialize distance matrix
matrix = np.zeros((N, N))
matrix_vectors = []
for m in range(N):
    matrix_vectors.append([])
    for n in range(N):
        matrix_vectors[m].append([])

def rssi_to_distance(rssi, tx_power, n=PATH_LOSS_N):
    return round(10 ** ((tx_power - rssi) / (10 * n)), 2)

def detection_callback(device, advertisement_data):
    service_uuids = advertisement_data.service_uuids or []
    uuid_match = any(TARGET_SERVICE_UUID.lower() == uuid.lower() for uuid in service_uuids)
    name_match = device.name and device.name.startswith(DEVICE_NAME_PREFIX)

    if uuid_match or name_match:
        raw = advertisement_data.service_data.get(TARGET_SERVICE_UUID)
        if not raw or len(raw) < 8:
            return

        tx_power = struct.unpack("b", raw[:1])[0]
        student_id = struct.unpack("b", raw[1:2])[0]
        tx_power_matrix[student_id] = tx_power
        seen_students.add(student_id)

        # Update 10 RSSI readings
        for i in range(10):
            peer_id = struct.unpack("b", raw[2 + (i*2):2 + (i*2) + 1])[0]
            if peer_id == 0:
                continue
            RSSI = struct.unpack("b", raw[2 + (i*2) + 1:2 + (i*2) + 2])[0]
            dist = rssi_to_distance(RSSI, tx_power_matrix[student_id])
            if dist > 0.5:
                matrix_vectors[student_id][peer_id].append(dist)
            # matrix_vectors[peer_id][student_id].append(dist)

        # Anchor node 0
        dist = rssi_to_distance(advertisement_data.rssi, tx_power)
        matrix_vectors[0][student_id].append(dist)
        # matrix_vectors[student_id][0].append(dist)
    fill_distance_matrix()    

def fill_distance_matrix(tolerance = 0.1):
    if not matrix_vectors:
        return
    for m in range(N):
        for n in range(m+1, N):
            if (len(matrix_vectors[m][n]) > 0) or (len(matrix_vectors[n][m]) > 0):
                matrix[m][n] = find_best_distances(matrix_vectors[m][n] + matrix_vectors[n][m], tolerance=tolerance)
                matrix[n][m] = matrix[m][n]

def find_best_distances(distance_list, tolerance = 0.1):
    if len(distance_list) > 0:
        distance_list = sorted(distance_list)
        # Step 1: Group clusters that fall in the same tolerance range
        clusters = []
        current_cluster = [distance_list[0]]
        for s in range(1, len(distance_list)):
            if abs(distance_list[s] - current_cluster[-1]) <= tolerance:
                current_cluster.append(distance_list[s])
            else:
                clusters.append(current_cluster)
                current_cluster = [distance_list[s]]
        clusters.append(current_cluster)

        most_common_cluster = max(clusters, key=len)
        return round(sum(most_common_cluster) / len(most_common_cluster), 2)
    return 0


async def scan_devices(scan_time=10.0):
    scanner = BleakScanner(detection_callback)
    await scanner.start()
    print("🔍 Scanning...")
    await asyncio.sleep(scan_time)
    await scanner.stop()
    print("✅ Scan finished.")

    # Optionally save results
    np.save("matrix.npy", matrix)
    np.save("tx_power.npy", tx_power_matrix)

    return matrix, seen_students

# Add to scanner.py
async def stream_scan(scan_time=10.0, step=2.0):
    scanner = BleakScanner(detection_callback)
    await scanner.start()
    print("🔍 Streaming BLE scan...")
    
    start_time = asyncio.get_event_loop().time()
    while (asyncio.get_event_loop().time() - start_time) < scan_time:
        await asyncio.sleep(step)
        yield seen_students.copy()  # yield copy to avoid mutation issues

    await scanner.stop()
    print("Stream scan finished.")

    np.save("matrix.npy", matrix)
    np.save("tx_power.npy", tx_power_matrix)

# For testing
if __name__ == "__main__":
    asyncio.run(scan_devices())
