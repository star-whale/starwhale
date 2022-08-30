from typing import Any, Set, Dict


class _DagData(object):
    """The internal data of DAG"""

    def __init__(self) -> None:
        self.__graph: Dict[Any, Set[Any]] = {}
        self.__graph_reverse: Dict[Any, Set[Any]] = {}

    def vertices(self) -> Set[Any]:
        """Get the vertices list"""
        return set(self.__graph.keys())

    def add_vertex(self, vertex: Any) -> None:
        """Add a new vertex"""
        if vertex not in self.__graph:
            self.__graph[vertex] = set()
            self.__graph_reverse[vertex] = set()

    def add_edge(self, v_from: Any, v_to: Any) -> None:
        """Add an edge from one vertex to another"""
        self.__graph[v_from].add(v_to)
        self.__graph_reverse[v_to].add(v_from)

    def remove_edge(self, v_from: Any, v_to: Any) -> None:
        """Remove an edge from one vertex to another"""
        self.__graph[v_from].remove(v_to)
        self.__graph_reverse[v_to].remove(v_from)

    def successors(self, vertex: Any) -> Set[Any]:
        """Get the successors of the specified vertex"""
        return self.__graph[vertex]

    def predecessors(self, vertex: Any) -> Set[Any]:
        """Get the predecessors of the specified vertex"""
        return self.__graph_reverse[vertex]


class DAG(object):
    def __init__(self) -> None:
        self.__data = _DagData()

    def __validate_vertex(self, *vertices: Any) -> None:
        for vtx in vertices:
            if vtx not in self.__data.vertices():
                raise RuntimeError(f"Vertex '{vtx}' does not belong to DAG")

    def __has_path_to(self, v_from: Any, v_to: Any) -> bool:
        if v_from == v_to:
            return True
        for vtx in self.__data.successors(v_from):
            if self.__has_path_to(vtx, v_to):
                return True
        return False

    def vertices(self) -> Set[Any]:
        """Get the vertices list"""
        return self.__data.vertices()

    def add_vertex(self, *vertices: Any) -> None:
        """Add one or more vertices"""
        for vtx in vertices:
            self.__data.add_vertex(vtx)

    def add_edge(self, v_from: Any, *v_tos: Any) -> None:
        """Add edge(s) from one vertex to others"""
        self.__validate_vertex(v_from, *v_tos)

        for v_to in v_tos:
            if self.__has_path_to(
                v_to, v_from
            ):  # pylint: disable=arguments-out-of-order
                raise RuntimeError(
                    f"If this edge from '{v_from}' to '{v_to}' is added, it will cause the graph to cycle"
                )
            self.__data.add_edge(v_from, v_to)

    def remove_edge(self, v_from: Any, v_to: Any) -> None:
        """Remove an edge from one vertex to another"""
        self.__validate_vertex(v_from, v_to)
        if v_to not in self.__data.successors(v_from):
            raise RuntimeError(f"Edge not found from '{v_from}' to '{v_to}'")

        self.__data.remove_edge(v_from, v_to)

    def vertex_size(self) -> int:
        """Get the size of vertices"""
        return len(self.__data.vertices())

    def edge_size(self) -> int:
        """Get the number of edges"""
        size = 0
        for vtx in self.__data.vertices():
            size += self.out_degree(vtx)
        return size

    def successors(self, vertex: Any) -> Set[Any]:
        """Get the successors of the vertex"""
        self.__validate_vertex(vertex)
        return self.__data.successors(vertex)

    def predecessors(self, vertex: Any) -> Set[Any]:
        """Get the predecessors of the vertex"""
        self.__validate_vertex(vertex)
        return self.__data.predecessors(vertex)

    def in_degree(self, vertex: Any) -> int:
        """Get the in degree of the vertex"""
        return len(self.predecessors(vertex))

    def out_degree(self, vertex: Any) -> int:
        """Get the out degree of the vertex"""
        return len(self.successors(vertex))

    def all_starts(self) -> Set[Any]:
        """Get all the starting vertices"""
        return set(vtx for vtx in self.__data.vertices() if self.in_degree(vtx) == 0)

    def all_terminals(self) -> Set[Any]:
        """Get all the terminating vertices"""
        return set(vtx for vtx in self.__data.vertices() if self.out_degree(vtx) == 0)
