import asyncio, math, struct
from bleak import BleakScanner

import numpy as np
import matplotlib.pyplot as plt
from sklearn.manifold import MDS
import networkx as nx

TARGET_SERVICE_UUID = "0000feed-0000-1000-8000-00805f9b34fb"
DEVICE_NAME_PREFIX = "IndoorScanner"

PATH_LOSS_N = 2.7          # tune per room
seen_students = set()
N = 30

tx_power_matrix = [0] * N
matrix = []
for k in range(N):
    t = []
    for m in range(N):
        t.append(0)
    matrix.append(t)

def rssi_to_distance(rssi, tx_power, n=PATH_LOSS_N):
    return round(10 ** ((tx_power - rssi) / (10 * n)), 2)

def detection_callback(device, advertisement_data):
    service_uuids = advertisement_data.service_uuids or []
    uuid_match = any(TARGET_SERVICE_UUID.lower() == uuid.lower() for uuid in service_uuids)
    name_match = device.name and device.name.startswith(DEVICE_NAME_PREFIX)

    if uuid_match or name_match:
        raw = advertisement_data.service_data.get(TARGET_SERVICE_UUID)
        tx_power = struct.unpack("b", raw[:1])[0]           # 1‑byte signed
        student_id = struct.unpack("b", raw[1:2])[0]
        tx_power_matrix[student_id] = tx_power

        for i in range(3):
            id = struct.unpack("b", raw[2 + (i*2):2 + (i*2) + 1])[0]
            if id == 0:
                continue
            RSSI = struct.unpack("b", raw[2 + (i*2) + 1:2 + (i*2) + 2])[0]
            # print("ID: ", id, " RSSI: ", RSSI)
            matrix[student_id][id] = rssi_to_distance(RSSI, tx_power_matrix[student_id])
            matrix[id][student_id] = rssi_to_distance(RSSI, tx_power_matrix[student_id])
            # matrix[id][student_id] = RSSI

        #student_id = raw[1:].decode(errors="ignore")
        # if student_id in seen_students:
        #     return  # Ignore duplicates

        seen_students.add(student_id)
        dist = rssi_to_distance(advertisement_data.rssi, tx_power)
        print(dist)
        matrix[0][student_id] = dist
        matrix[student_id][0] = dist

        print(f"{student_id} RSSI:{advertisement_data.rssi}dBm  ≈{dist}m")

async def run():
    scanner = BleakScanner(detection_callback)
    await scanner.start()
    print("🔍 Scanning for BLE devices...")
    await asyncio.sleep(40.0)
    await scanner.stop()
    print("🔍 Scan finished.")
    print('tx_power_matrix[26]: ', tx_power_matrix[26], 'tx_power_matrix[29]: ', tx_power_matrix[29])
    print('matrix[26][29]: ', matrix[26][29], 'matrix[29][26]: ', matrix[29][26])

asyncio.run(run())
print(matrix)

# Step 1: Apply MDS to project distances into 2D coordinates
mds = MDS(n_components=2, dissimilarity='precomputed', random_state=42)
coords = mds.fit_transform(matrix)
print(coords)

# Step 2: Build the graph from the distance matrix
fixed_node_index = 0
offset = coords[fixed_node_index]  # original position of node 0
coords -= offset                   # translate so node 0 is at origin

# Step 3: Reflect all other nodes above the x-axis (y >= 0)
for i in range(len(coords)):
    if i != fixed_node_index and coords[i][1] < 0:
        coords[i][1] = -coords[i][1]

# Step 4: Create graph
G = nx.Graph()
n = len(matrix)

for i in range(n):
    for j in range(i + 1, n):
        if matrix[i][j] > 0:
            G.add_edge(i, j, weight=matrix[i][j])

# Step 5: Map coordinates to node positions
pos = {i: coords[i] for i in range(n)}

# Step 6: Plot
plt.figure(figsize=(8, 6))
nx.draw(G, pos, with_labels=True, node_color='lightgreen', node_size=900, font_size=12)
edge_labels = nx.get_edge_attributes(G, 'weight')
nx.draw_networkx_edge_labels(G, pos, edge_labels=edge_labels, font_size=10)
plt.title("Graph with Node 0 Fixed at Bottom Center")
plt.axis('equal')
plt.grid(True)
plt.show()
