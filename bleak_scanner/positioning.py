# positioning.py
import numpy as np
from sklearn.manifold import MDS

def compute_positions(matrix, fixed_node=0):
    # Apply MDS
    mds = MDS(n_components=2, dissimilarity='precomputed', random_state=42)
    coords = mds.fit_transform(matrix)

    # Align with fixed node at origin

    offset = coords[fixed_node]
    coords -= offset
    # Reflect below-x-axis nodes
    for i in range(len(coords)):
        if i != fixed_node and coords[i][1] < 0:
            coords[i][1] = -coords[i][1]

    # Save for dashboard use
    np.save("coords.npy", coords)
    return coords

# For testing
if __name__ == "__main__":
    coords = compute_positions()

# from scipy.spatial.distance import squareform
# from scipy.linalg import eigh

# def classical_mds(D, k=2):
#     n = D.shape[0]
#     D2 = D ** 2
#     J = np.eye(n) - np.ones((n, n)) / n
#     B = -0.5 * J @ D2 @ J

#     eigvals, eigvecs = eigh(B)
#     idx = np.argsort(eigvals)[::-1]
#     eigvals, eigvecs = eigvals[idx], eigvecs[:, idx]

#     L = np.diag(np.sqrt(eigvals[:k]))
#     V = eigvecs[:, :k]

#     return V @ L

# # Use:
# positions = classical_mds(matrix)
# print(positions)
