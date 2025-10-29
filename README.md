# BLE-Based Smart Attendance & Localization System  

A **Bluetooth Low Energy (BLE)**-powered system that enables **real-time attendance tracking** and **indoor localization** using Android devices and a laptop-based scanner.

Built as part of a **Pervasive and Mobile Computing** course project, this work combines **wireless sensing**, **distance estimation**, and **multidimensional scaling (MDS)** to visualize peer positions dynamically.

---

## Features

- **localization** using Multidimensional Scaling (MDS)  
- **RSSI-based distance estimation** with adaptive clustering filter  
- **Real-time visualization dashboard** using Streamlit  
- **Android BLE broadcaster and scanner app** for student devices  
- **Automated attendance marking** based on signal proximity  
- **Cross-platform deployment** (Android & Windows/Linux laptop)  

---

## System Architecture

**Components:**
1. **Android Phones:** Broadcast BLE advertisement packets with Student ID, Tx Power, and timestamp.  
2. **Bleak Scanner:** Continuously scans BLE signals using `BleakScanner`, extracts RSSI values, and estimates distances.  
3. **Computation Layer:** Converts RSSI readings to distances, applies MDS for coordinate generation, and plots spatial graphs.  
4. **Visualization Layer:** Streamlit dashboard updates dynamically to show node positions and attendance records.

**Diagrams:**
1. **System Architecture**:

<img width="2848" height="1801" alt="BLE" src="https://github.com/user-attachments/assets/8808bc96-2d37-4231-a6d9-0de79d56195f" />

2. **Payload Structure**:

<img width="577" height="432" alt="table" src="https://github.com/user-attachments/assets/41d29917-2ed5-4937-a633-be46f32443fa" />

3. **Localization Matrix**:

<img width="379" height="344" alt="matrix" src="https://github.com/user-attachments/assets/f0657a4c-a05e-4d22-8c7a-994c0af4fa6e" />

4. **Streamlit Dashboard**:

<img width="1785" height="452" alt="Screenshot 2025-07-31 204349" src="https://github.com/user-attachments/assets/3e57f608-265f-4327-96a9-936114b35a73" />

5. **Android App UI**:

<img width="540" height="812" alt="Screenshot_20250729-223948" src="https://github.com/user-attachments/assets/2f76e3e5-d739-49cc-bee6-61f81c00c3f5" />

<img width="540" height="812" alt="Screenshot_20250729-223955" src="https://github.com/user-attachments/assets/de15c796-2fb0-409d-9103-cf78b314b70f" />

6. **Localization Results**:

<img width="1031" height="779" alt="43e7d1d3e30b9e12e0c358bb017b50f233c8d2dceee91baabc39551f" src="https://github.com/user-attachments/assets/4ddcd68b-22cd-4c8b-8815-48492c247e54" />

<img width="1031" height="779" alt="a0a15a4edc5b51a5b3408547b9fd6892c7097b6f59eca66e436e245d" src="https://github.com/user-attachments/assets/4c3fe441-69f4-4509-a3ab-20f833a01899" />

<img width="1031" height="779" alt="ddf3f2e4fa45c5de99cc25f5e5ce4d9d3f5db95dc1ec73dcc7183555" src="https://github.com/user-attachments/assets/2bfda0e3-fb64-4312-8fad-1b59c6b40647" />
