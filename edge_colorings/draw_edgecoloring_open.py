from tkinter import Tk, IntVar, Checkbutton, Frame
import matplotlib.backends.backend_tkagg as tkagg
import math
import tkinter as tk
import networkx as nx
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from tkinter import Tk, IntVar, Checkbutton, Frame, mainloop, Canvas
import numpy as np
import tkinter as tk
from tkinter import filedialog
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import numpy as np
import math
import networkx as nx


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


class GraphRendererApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Graph Renderer")

        self.figure, self.ax = plt.subplots(figsize=(8, 10))
        self.figure.subplots_adjust(right=0.8)
        self.canvas_widget = FigureCanvasTkAgg(self.figure, master=root)
        self.canvas_widget.get_tk_widget().grid(row=0, column=0, sticky="nsew")

        # Adjust the aspect ratio to make the graph circular
        self.ax.set_aspect("equal", adjustable="box")

        self.color_checkboxes = []
        self.select_all_checkbox = 0

        self.frame = tk.Frame(root)
        self.frame.grid(row=0, column=1, sticky="nsew")
        self.frame.grid_columnconfigure(0, weight=1)

        # Add an "Open" button
        self.open_button = tk.Button(
            self.frame, text="Open File", command=self.open_file_dialog
        )

        # Nuevo botón: Exportar a TikZ
        self.export_button = tk.Button(
            self.frame, text="Export TikZ", command=self.export_to_tikz
        )

        # Initialize average number of connections
        self.avgNumConnections = 0

        # Label to display average number of connections
        self.avg_connections_label = tk.Label(
            self.frame, text=f"Avg Num Connections: {self.avgNumConnections}"
        )

        # Inicializar atributos para exportar
        self.current_graph = None
        self.positions = None

        self.open_file_dialog()

        # Initially, render all color codes
        self.update_graph()

    def open_file_dialog(self):
        # Open file dialog to select a file
        file_path = filedialog.askopenfilename(
            title="Select File",
            filetypes=[("Text files", "*.txt"), ("All files", "*.*")],
        )
        if file_path:
            # Process the selected file
            self.process_file(file_path)

    def process_file(self, file_path):
        # Read the number of vertices and color codes from the selected file
        with open(file_path, "r") as file:
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

        # Recalculate average number of connections
        self.calculate_avg_connections()

        # Update the UI
        self.update_ui()

    def calculate_avg_connections(self):
        # Calculate average number of connections between pairs of used colors
        numPairs = 0
        sumConnections = 0
        for a, colorA in enumerate(self.unique_color_codes):
            for b, colorB in enumerate(self.unique_color_codes):
                if a < b:
                    index = 0
                    pairVertices = [set({}), set({})]
                    for i in range(len(self.vertices)):
                        for j in range(len(self.vertices)):
                            if i < j:
                                color = self.edge_coloring[index]
                                index = index + 1
                                if a == color:
                                    pairVertices[0].update({i})
                                    pairVertices[0].update({j})
                                if b == color:
                                    pairVertices[1].update({i})
                                    pairVertices[1].update({j})

                    commonVertices = pairVertices[0].intersection(pairVertices[1])
                    sumConnections += len(commonVertices)
                    numPairs = numPairs + 1
        self.avgNumConnections = sumConnections / numPairs

    def update_ui(self):
        self.open_button.grid(
            row=len(self.unique_color_codes) + 2, column=0, padx=5, pady=5, sticky="w"
        )
        # Nuevo: colocar el botón de exportación debajo del de abrir
        self.export_button.grid(
            row=len(self.unique_color_codes) + 3, column=0, padx=5, pady=5, sticky="w"
        )

        # Update the label displaying average number of connections
        self.avg_connections_label.grid(
            row=len(self.unique_color_codes), column=0, sticky="w"
        )
        self.avg_connections_label.config(
            text=f"Avg Num Connections: {self.avgNumConnections}"
        )

        # Clear existing checkboxes
        for checkbox, _, _ in self.color_checkboxes:
            checkbox.grid_remove()

        # Create checkboxes for each color code
        self.color_checkboxes = []
        initial_checked_values = [1] * len(self.unique_color_codes)

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
            self.color_checkboxes.append((checkbox, code, var))

        # Create a "Select All" checkbox and set its initial value to 0 (unchecked)

        self.select_all_var = IntVar(value=1)
        if self.select_all_checkbox != 0:
            self.select_all_checkbox.grid_remove()
        self.select_all_checkbox = Checkbutton(
            self.frame,
            text="Select All",
            variable=self.select_all_var,
            command=self.toggle_select_all,
        )
        self.select_all_checkbox.grid(
            row=len(self.unique_color_codes) // num_columns + 1, column=0, sticky="w"
        )
        # Update the graph
        self.update_graph()

    def toggle_select_all(self):
        # Get the value of the "Select All" checkbox
        select_all_value = self.select_all_var.get()

        # Set the value of all other checkboxes to match the "Select All" checkbox
        for _, _, var in self.color_checkboxes:
            var.set(select_all_value)

        # Update the graph based on the new checkbox values
        self.update_graph()

    # Remaining methods (create_custom_graph_with_colors, update_graph, etc.) remain unchanged
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
            code for _, code, var in self.color_checkboxes if var.get() == 1
        ]
        custom_graph = self.create_custom_graph_with_colors(
            len(self.vertices), self.edge_coloring, selected_color_codes
        )

        # Guardar para exportar
        self.current_graph = custom_graph

        # Create a circular layout with vertex 0 at the top
        positions = {}
        angle = 360.0 / len(self.vertices)
        for i, vertex in enumerate(self.vertices):
            x = math.cos(math.radians(90 - i * angle))
            y = math.sin(math.radians(90 - i * angle)) + len(selected_color_codes) / 5 * 1
            positions[vertex] = (x, y)

        # Guardar posiciones para TikZ
        self.positions = positions

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
            ncol=5,
            bbox_to_anchor=(1.3, 1.4),
        )

        # Add a delay to allow the plot to be displayed
        self.root.after(100, self.canvas_widget.draw)

    def export_to_tikz(self):
        # Si aún no hay grafo/posiciones, no hacemos nada
        if self.current_graph is None or self.positions is None:
            return

        file_path = filedialog.asksaveasfilename(
            title="Save TikZ File",
            defaultextension=".tex",
            filetypes=[("TeX files", "*.tex"), ("All files", "*.*")]
        )
        if not file_path:
            return

        lines = []
        lines.append("% TikZ export generated by GraphRendererApp")
        lines.append("% Requires: \\usepackage{tikz}")
        lines.append("\\begin{tikzpicture}[scale=3, every node/.style={circle, draw, fill=lightgray, minimum size=1cm}]")

        # Nodos
        for v, (x, y) in self.positions.items():
            lines.append(f"  \\node ({v}) at ({x:.4f},{y:.4f}) {{{v}}};")

        lines.append("")

        # Aristas
        for u, v, data in self.current_graph.edges(data=True):
            color = data.get("color", "black")
            style = data.get("style", "solid")
            width = data.get("width", 1.0)

            opts = []
            if color:
                opts.append(color)
            if style and style != "solid":
                opts.append(style)
            if width:
                opts.append(f"line width={width:.2f}pt")

            if opts:
                opt_str = ", ".join(opts)
                lines.append(f"  \\draw[{opt_str}] ({u}) -- ({v});")
            else:
                lines.append(f"  \\draw ({u}) -- ({v});")

        lines.append("\\end{tikzpicture}")

        with open(file_path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines))


if __name__ == "__main__":
    root = Tk()
    app = GraphRendererApp(root)
    root.mainloop()
