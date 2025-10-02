import asyncio
from bleak import BleakScanner

# Constants
SERVICE_UUID = "0000feed-0000-1000-8000-00805f9b34fb"
N_PATH_LOSS = 2.0  # Environmental constant

# Estimate distance using RSSI and TxPower
def estimate_distance(rssi: int, tx_power: int, n: float = N_PATH_LOSS) -> float:
    return round(10 ** ((tx_power - rssi) / (10 * n)), 2)

# Extract student ID and TxPower from service data
def parse_payload(advertisement_data):
    service_data = advertisement_data.service_data.get(SERVICE_UUID.lower(), None)
    if service_data and len(service_data) >= 2:
        tx_power = int.from_bytes(service_data[0:1], byteorder='big', signed=True)
        student_id = service_data[1:].decode("utf-8", errors="ignore")
        return tx_power, student_id
    else:
        print('Error')
    return None, None

async def main():
    print("🔎 Scanning for BLE advertisements...")
    devices = await BleakScanner.discover(timeout=10)
    print(len(devices))
    for d in devices:
        ad_data = d.metadata.get("advertisement_data", None)
        if not ad_data:
            print('not ad_data')
            continue

        tx_power, student_id = parse_payload(ad_data)
        if tx_power is not None and student_id:
            rssi = d.rssi
            distance = estimate_distance(rssi, tx_power)

            print(f"🔍 Device: {d.address}")
            print(f"   Student ID: {student_id}")
            print(f"   RSSI: {rssi} dBm | TxPower: {tx_power} dBm")
            print(f"   📏 Estimated Distance: {distance} meters")
            print("-" * 40)
        else:
            print('not tx_power | student_id')

    print("✅ Scan complete.")

if __name__ == "__main__":
    asyncio.run(main())
