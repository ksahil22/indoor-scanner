import streamlit as st
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import networkx as nx
import asyncio

from scanner import stream_scan
from positioning import compute_positions

seen_overall = set()

# ---------------------------
# Load Static Student List
# ---------------------------
@st.cache_data
def load_student_data():
    df = pd.read_csv("student_data.csv")
    df['Status'] = 'Absent'
    return df

# ---------------------------
# Status Highlighter
# ---------------------------
def highlight_status(val):
    color = 'green' if val == "Present" else 'red'
    return f'background-color: {color}; color: white'

# ---------------------------
# Main Streamlit App
# ---------------------------
st.set_page_config("BLE Attendance Dashboard", layout="wide")
st.title("BLE Attendance & Localization Dashboard")

# Load session state
if 'df' not in st.session_state:
    st.session_state.df = load_student_data()
if 'scan_done' not in st.session_state:
    st.session_state.scan_done = False

# Show table
st.subheader("Attendance Table")
styled_df = st.session_state.df.style.applymap(highlight_status, subset=['Status'])

st.markdown("""
    <style>
        thead tr th:first-child {display:none}
        tbody th {display:none}
    </style>
""", unsafe_allow_html=True)
placeholder = st.dataframe(styled_df, use_container_width=True)

# # Scan Button
# if st.button("🔍 Start BLE Scan"):
#     with st.spinner("Scanning for BLE advertisements..."):
#         matrix, seen_students = asyncio.run(scan_devices())

#         # Update status
#         for sid in seen_students:
#             st.session_state.df.loc[
#                 st.session_state.df['roll_number'] == sid, 'Status'
#             ] = "Present"

#         st.session_state.scan_done = True
#         st.session_state.matrix = matrix

#     st.success("✅ Scan complete and attendance updated.")

# if st.button("🔍 Start BLE Scan (Live Updates)"):
    # placeholder = st.empty()
with st.spinner("Streaming BLE scan..."):
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    async def run_stream():
        async for seen_students in stream_scan(scan_time=20, step=2):
            seen_overall.update(seen_students)
            print("seen_overall", seen_overall)
            for sid in seen_overall:
                st.session_state.df.loc[
                    st.session_state.df['roll_number'] == sid, 'Status'
                ] = "Present"
            # Show updated table
            styled_df = st.session_state.df.style.applymap(highlight_status, subset=['Status'])
            with placeholder:
                st.markdown("""
                    <style>
                        thead tr th:first-child {display:none}
                        tbody th {display:none}
                    </style>
                """, unsafe_allow_html=True)
                st.dataframe(styled_df, use_container_width=True)

        return seen_overall

    seen_set = loop.run_until_complete(run_stream())
    loop.close()

    st.session_state.scan_done = True
    st.session_state.matrix = np.load("matrix.npy")

# Localization Graph (after scan)
if st.session_state.get('scan_done'):
    st.subheader("Device Location Graph")
    matrix = st.session_state.matrix

    coords = compute_positions(matrix)
    
    # Build graph
    G = nx.Graph()
    N = matrix.shape[0]
    print("N: ", N)
    print(coords)
    # G = nx.relabel_nodes(G, {0: 'Anchor'})
    for i in range(N):
        for j in range(i + 1, N):
            if matrix[i][j] > 0 and ((i in seen_overall or i == 0) and (j in seen_overall or j == 0)):
                G.add_edge(i, j, weight=round(matrix[i][j], 2))
    pos = {i: coords[i] for i in range(len(coords)) if i in seen_overall or i == 0}

    fig, ax = plt.subplots()
    
    nx.draw(G, pos, with_labels=True, node_color='lightblue', node_size=800, font_size=10, ax=ax)
    nx.draw_networkx_edge_labels(G, pos, edge_labels={(i, j): f"{matrix[i][j]:.1f}" for i, j in G.edges()}, ax=ax)

    st.pyplot(fig)

# Footer
st.caption("Built with Streamlit • BLE + MDS for Indoor Localization")
