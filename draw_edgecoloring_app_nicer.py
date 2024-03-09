from tkinter import Tk, IntVar, Checkbutton, Frame
import matplotlib.backends.backend_tkagg as tkagg
import math
import tkinter as tk
import networkx as nx
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from tkinter import Tk, IntVar, Checkbutton, Frame, mainloop, Canvas
import numpy as np


def assign_attributes(edge_coloring):
    unique_color_set = set(edge_coloring)
    # Valid options for colors, styles, and widths
    valid_colors = [
        "red",
        "green",
        "blue",
        "purple",
        "orange",
        "cyan",
        "pink",
        "brown",
        "magenta",
        "gray",
        "black",
    ]
    valid_styles = ["solid", "dotted", "dashed"]
    valid_widths = [2.0, 4.0]

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


# ... (Previous code for assign_attributes and create_custom_graph_with_colors functions) ...


# ... (Previous code for assign_attributes and create_custom_graph_with_colors functions) ...


class GraphRendererApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Graph Renderer")

        self.figure, self.ax = plt.subplots(figsize=(8, 8))  # Create a subplot

        # After creating the subplot, adjust its position within the figure
        # Adjust the left parameter as needed
        self.figure.subplots_adjust(right=0.8)
        self.canvas_widget = FigureCanvasTkAgg(self.figure, master=root)
        self.canvas_widget.get_tk_widget().grid(
            row=0, column=0, sticky="nsew"
        )  # Place canvas on the left

        # Adjust the aspect ratio to make the graph circular
        self.ax.set_aspect("equal", adjustable="box")

        self.color_checkboxes = []

        self.frame = tk.Frame(root)
        # Place frame on the right
        self.frame.grid(row=0, column=1, sticky="nsew")
        # Allow frame to expand vertically
        self.frame.grid_columnconfigure(0, weight=1)

        # Read the number of vertices and color codes from the file
        with open("edge_coloring_12_21.txt", "r") as file:
            # Read the number of vertices
            vertices_line = file.readline().strip()
            self.vertices = list(range(int(vertices_line)))

            # Initialize an empty list to store color codes
            self.edge_coloring = []

            # Read color codes as integers, split by spaces and newlines
            for line in file:
                color_codes_line = line.strip()
                if color_codes_line:
                    self.edge_coloring.extend(
                        [int(code) for code in color_codes_line.split()]
                    )

        # Get the unique color codes, sort them, and initialize them as checked
        self.unique_color_codes = sorted(list(set(self.edge_coloring)))
        initial_checked_values = [0] * len(self.unique_color_codes)

        # Get average number of connections between pairs of used colors
        numPairs=0
        sumConnections = 0
        for a, colorA in enumerate(self.unique_color_codes):
            for b, colorB in enumerate(self.unique_color_codes):
                if a < b:
                    index = 0
                    pairVertices = [set({}),set({})]
                    for i in range(len(self.vertices)):
                        for j in range(len(self.vertices)):
                            if i<j:
                                color = self.edge_coloring[index]
                                index = index+1
                                if a == color:
                                    pairVertices[0].update({i})
                                    pairVertices[0].update({j})
                                if b == color:
                                    pairVertices[1].update({i})
                                    pairVertices[1].update({j})
                    
                    commonVertices = pairVertices[0].intersection(pairVertices[1])
                    sumConnections +=len(commonVertices)
                    numPairs = numPairs+1
        self.avgNumConnections = sumConnections / numPairs

        # Label to display avgNumConnections
        self.avg_connections_label = tk.Label(
            self.frame, text=f"Avg Num Connections: {self.avgNumConnections}"
        )
        self.avg_connections_label.grid(row=len(self.unique_color_codes), column=0, sticky="w")

        # Create checkboxes for each color code
        num_columns = 2  # Number of columns for checkboxes
        for i, (code, initial_value) in enumerate(
            zip(self.unique_color_codes, initial_checked_values)
        ):
            var = IntVar(value=initial_value)
            checkbox = Checkbutton(
                self.frame,
                text=f"Color {code}",
                variable=var,
                command=self.update_graph,
            )
            # Place checkboxes in two columns
            checkbox.grid(row=i // num_columns, column=i % num_columns, sticky="w")
            self.color_checkboxes.append((code, var))

        # Create a "Select All" checkbox and set its initial value to 0 (unchecked)
        self.select_all_var = IntVar(value=0)
        select_all_checkbox = Checkbutton(
            self.frame,
            text="Select All",
            variable=self.select_all_var,
            command=self.toggle_select_all,
        )
        select_all_checkbox.grid(
            row=len(self.unique_color_codes) // num_columns + 1, column=0, sticky="w"
        )

        # Initially, render all color codes
        self.update_graph()

    def toggle_select_all(self):
        # Get the value of the "Select All" checkbox
        select_all_value = self.select_all_var.get()

        # Set the value of all other checkboxes to match the "Select All" checkbox
        for _, var in self.color_checkboxes:
            var.set(select_all_value)

        # Update the graph based on the new checkbox values
        self.update_graph()

    def create_custom_graph_with_colors(
        self, vertices, edge_coloring, color_codes_to_render
    ):
        G = nx.Graph()

        # Add nodes based on the number of vertices
        G.add_nodes_from(range(vertices))

        # Determine the number of edges based on the number of nodes
        num_edges = vertices * (vertices - 1) // 2

        # Get triplets (color, style, width) for each color code
        self.triplets = assign_attributes(edge_coloring)

        # Add edges with custom attributes based on triplets and specified color codes
        edges = []
        index = 0
        for i in range(vertices):
            for j in range(i + 1, vertices):
                edge_color, edge_style, edge_width = self.triplets[edge_coloring[index]]

                # Check if the current edge color is in the list of color codes to render
                if edge_coloring[index] in color_codes_to_render:
                    edges.append(
                        (
                            i,
                            j,
                            {
                                "color": edge_color,
                                "style": edge_style,
                                "width": edge_width,
                            },
                        )
                    )

                index = index + 1

        G.add_edges_from(edges)

        return G

    def update_graph(self):
        # Clear the subplot
        self.ax.clear()

        # Get the graph based on selected color codes
        selected_color_codes = [
            code for code, var in self.color_checkboxes if var.get() == 1
        ]
        custom_graph = self.create_custom_graph_with_colors(
            len(self.vertices), self.edge_coloring, selected_color_codes
        )

        # Create a circular layout with vertex 0 at the top
        positions = {}
        angle = 360.0 / len(self.vertices)
        for i, vertex in enumerate(self.vertices):
            x = math.cos(math.radians(90 - i * angle))
            y = math.sin(math.radians(90 - i * angle))
            positions[vertex] = (x, y)

        # Define custom attributes for edges
        edge_attributes = nx.get_edge_attributes(custom_graph, "color")
        edge_colors = [edge_attributes[edge] for edge in custom_graph.edges()]
        edge_attributes = nx.get_edge_attributes(custom_graph, "style")
        edge_styles = [edge_attributes[edge] for edge in custom_graph.edges()]
        edge_attributes = nx.get_edge_attributes(custom_graph, "width")
        edge_widths = [edge_attributes[edge] for edge in custom_graph.edges()]

        # Draw the graph with custom attributes and specified positions on the subplot
        nx.draw(
            custom_graph,
            positions,
            ax=self.ax,
            with_labels=True,
            node_color="lightgray",
            font_weight="bold",
            node_size=1000,
            font_size=12,
            edge_color=edge_colors,
            style=edge_styles,
            width=edge_widths,
            cmap=plt.cm.rainbow,
            font_color="black",
            arrows=False,
        )  # Disable arrows for an undirected graph

        # Create a legend for edge attributes using only unique color codes in the graph
        unique_color_codes = list(set(selected_color_codes))
        legend_handles = []

        for i, code in enumerate(unique_color_codes):
            # Get the corresponding triplet for the color code
            color, style, width = self.triplets[code]

            # Create a legend entry with the color, style, and width
            legend_handles.append(
                plt.Line2D(
                    [0],
                    [0],
                    color=color,
                    linestyle=style,
                    linewidth=width,
                    label=f"Color {code}",
                )
            )

        # Specify the position and appearance of the legend
        plt.legend(
            handles=legend_handles,
            title="Color codes",
            loc="upper right",
            handlelength=3,
            ncol=2,
            bbox_to_anchor=(1.3, 1.25),
        )

        # Add a delay to allow the plot to be displayed
        self.root.after(100, self.canvas_widget.draw)


if __name__ == "__main__":
    root = Tk()
    app = GraphRendererApp(root)
    root.mainloop()
