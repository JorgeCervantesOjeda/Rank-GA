import networkx as nx
import matplotlib.pyplot as plt
import matplotlib.lines as mlines  # Import mlines module
import math

# Function to assign colors, styles, and widths based on prime values


def assign_attributes(edge_coloring):
    unique_color_set = set(edge_coloring)
    # Valid options for colors, styles, and widths
    valid_colors = ['red', 'green', 'blue', 'purple', 'orange',
                    'cyan', 'pink', 'brown', 'magenta', 'gray', 'black']
    valid_styles = ['solid', 'dotted', 'dashed']
    valid_widths = [2.0, 3.0, 4.0, 5.0]

    nc = len(valid_colors)  # Number of valid colors
    ns = len(valid_styles)  # Number of valid styles
    nw = len(valid_widths)  # Number of valid widths

    triplets = []

    for edge_color in unique_color_set:
        # Assign colors cyclically from the list of valid colors
        color = valid_colors[edge_color % nc]

        # Assign styles cyclically from the list of valid styles
        style = valid_styles[edge_color % ns]

        # Assign widths cyclically from the list of valid widths
        width = valid_widths[edge_color % nw]

        triplets.append((color, style, width))

    return triplets


def create_custom_graph_with_colors(vertices, edge_coloring):
    G = nx.Graph()

    # Add nodes based on the number of vertices
    G.add_nodes_from(range(vertices))

    # Determine the number of edges based on the number of nodes
    num_edges = vertices * (vertices - 1) // 2

    # Get triplets (color, style, width) for each color code
    triplets = assign_attributes(edge_coloring)

    # Add edges with custom attributes based on triplets
    edges = []
    index = 0
    for i in range(vertices):
        for j in range(i + 1, vertices):
            edge_color, edge_style, edge_width = triplets[edge_coloring[index]]
            edges.append(
                (i, j, {'color': edge_color, 'style': edge_style, 'width': edge_width}))
            index = index + 1

    G.add_edges_from(edges)

    return G


def create_custom_graph_with_colors(vertices, edge_coloring, color_code_to_render):
    G = nx.Graph()

    # Add nodes based on the number of vertices
    G.add_nodes_from(range(vertices))

    # Determine the number of edges based on the number of nodes
    num_edges = vertices * (vertices - 1) // 2

    # Get triplets (color, style, width) for each color code
    triplets = assign_attributes(edge_coloring)

    # Add edges with custom attributes based on triplets
    edges = []
    index = 0
    for i in range(vertices):
        for j in range(i + 1, vertices):
            edge_color, edge_style, edge_width = triplets[edge_coloring[index]]

            # Check if the current edge color matches the specified color code to render
            if edge_coloring[index] == color_code_to_render:
                edges.append(
                    (i, j, {'color': edge_color, 'style': edge_style, 'width': edge_width}))

            index = index + 1

    G.add_edges_from(edges)

    return G


def create_custom_graph_with_colors(vertices, edge_coloring, color_codes_to_render):
    G = nx.Graph()

    # Add nodes based on the number of vertices
    G.add_nodes_from(range(vertices))

    # Determine the number of edges based on the number of nodes
    num_edges = vertices * (vertices - 1) // 2

    # Get triplets (color, style, width) for each color code
    triplets = assign_attributes(edge_coloring)

    # Add edges with custom attributes based on triplets and specified color codes
    edges = []
    index = 0
    for i in range(vertices):
        for j in range(i + 1, vertices):
            edge_color, edge_style, edge_width = triplets[edge_coloring[index]]

            # Check if the current edge color is in the list of color codes to render
            if edge_coloring[index] in color_codes_to_render:
                edges.append(
                    (i, j, {'color': edge_color, 'style': edge_style, 'width': edge_width}))

            index = index + 1

    G.add_edges_from(edges)

    return G


def visualize_custom_graph_with_colors(G, vertices):
    # Define custom attributes for edges
    edge_attributes = nx.get_edge_attributes(G, 'color')
    edge_colors = [edge_attributes[edge] for edge in G.edges()]
    edge_attributes = nx.get_edge_attributes(G, 'style')
    edge_styles = [edge_attributes[edge] for edge in G.edges()]
    edge_attributes = nx.get_edge_attributes(G, 'width')
    edge_widths = [edge_attributes[edge] for edge in G.edges()]

    # Create a circular layout with vertex 0 at the top
    positions = {}
    angle = 360.0 / len(vertices)
    for i, vertex in enumerate(vertices):
        x = math.cos(math.radians(90 - i * angle))
        y = math.sin(math.radians(90 - i * angle))
        positions[vertex] = (x, y)

    # Draw the graph with custom attributes and specified positions
    plt.figure(figsize=(8, 8))  # Adjust the figure size if needed
    nx.draw(G, positions, with_labels=True, node_color='lightgray', font_weight='bold',
            node_size=1000, font_size=12,
            edge_color=edge_colors, style=edge_styles, width=edge_widths,
            cmap=plt.cm.rainbow, font_color='black', arrows=False)  # Disable arrows for undirected graph

    # Create a legend for edge attributes using only unique color codes in the graph
    unique_color_codes = list(set(edge_coloring))
    legend_handles = []

    for i, code in enumerate(unique_color_codes):
        # Get the corresponding triplet for the color code
        color, style, width = assign_attributes([code])[0]

        # Create a legend entry with the color, style, and width
        legend_handles.append(
            plt.Line2D([0], [0], color=color, linestyle=style,
                       linewidth=width, label=f'{code}')
        )

    plt.legend(handles=legend_handles,
               title='Color codes', loc='upper right', handlelength=3)

    plt.show()


# Example usage:
if __name__ == "__main__":
    # Read the number of vertices and color codes from the file
    with open('edge_coloring_20_39.txt', 'r') as file:
        # Create a list of vertices
        vertices = list(range(int(file.readline().strip())))
        # Read color codes as integers
        edge_coloring = [int(code) for code in file.readline().split()]

    # Create a custom graph with associated colors, styles, and widths
    custom_graph = create_custom_graph_with_colors(
        len(vertices), edge_coloring, [0, 1, 2, 3, 4, 5])

    # Visualize the custom graph with distinct colors, line styles, and line widths
    visualize_custom_graph_with_colors(custom_graph, vertices)
