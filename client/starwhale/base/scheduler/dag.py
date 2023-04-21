from typing import Any, Set, Dict


class _DAGData:
    def __init__(self) -> None:
        self.__graph: Dict[Any, Set[Any]] = {}
        self.__graph_reverse: Dict[Any, Set[Any]] = {}

    def vertices(self) -> Set[Any]:
        return set(self.__graph.keys())

    def add_vertex(self, vertex: Any) -> None:
        if vertex not in self.__graph:
            self.__graph[vertex] = set()
            self.__graph_reverse[vertex] = set()

    def add_edge(self, v_from: Any, v_to: Any) -> None:
        self.__graph[v_from].add(v_to)
        self.__graph_reverse[v_to].add(v_from)

    def remove_edge(self, v_from: Any, v_to: Any) -> None:
        self.__graph[v_from].remove(v_to)
        self.__graph_reverse[v_to].remove(v_from)

    def successors(self, vertex: Any) -> Set[Any]:
        return self.__graph[vertex]

    def predecessors(self, vertex: Any) -> Set[Any]:
        return self.__graph_reverse[vertex]


class DAG:
    def __init__(self) -> None:
        self.__data = _DAGData()

    def _validate_vertex(self, *vertices: Any) -> None:
        for vtx in vertices:
            if vtx not in self.__data.vertices():
                raise RuntimeError(f"Vertex '{vtx}' does not belong to DAG")

    def _has_path_to(self, v_from: Any, v_to: Any) -> bool:
        if v_from == v_to:
            return True
        for vtx in self.__data.successors(v_from):
            if self._has_path_to(vtx, v_to):
                return True
        return False

    def vertices(self) -> Set[Any]:
        return self.__data.vertices()

    def add_vertex(self, *vertices: Any) -> None:
        for vtx in vertices:
            self.__data.add_vertex(vtx)

    def add_edge(self, v_from: Any, *v_tos: Any) -> None:
        self._validate_vertex(v_from, *v_tos)

        for v_to in v_tos:
            if self._has_path_to(v_to, v_from):
                raise RuntimeError(
                    f"If this edge from '{v_from}' to '{v_to}' is added, it will cause the graph to cycle"
                )
            self.__data.add_edge(v_from, v_to)

    def remove_edge(self, v_from: Any, v_to: Any) -> None:
        self._validate_vertex(v_from, v_to)
        if v_to not in self.__data.successors(v_from):
            raise RuntimeError(f"Edge not found from '{v_from}' to '{v_to}'")

        self.__data.remove_edge(v_from, v_to)

    def vertex_size(self) -> int:
        return len(self.__data.vertices())

    def edge_size(self) -> int:
        size = 0
        for vtx in self.__data.vertices():
            size += self.out_degree(vtx)
        return size

    def successors(self, vertex: Any) -> Set[Any]:
        self._validate_vertex(vertex)
        return self.__data.successors(vertex)

    def predecessors(self, vertex: Any) -> Set[Any]:
        self._validate_vertex(vertex)
        return self.__data.predecessors(vertex)

    def in_degree(self, vertex: Any) -> int:
        return len(self.predecessors(vertex))

    def out_degree(self, vertex: Any) -> int:
        return len(self.successors(vertex))

    def all_starts(self) -> Set[Any]:
        return set(vtx for vtx in self.__data.vertices() if self.in_degree(vtx) == 0)

    def all_terminals(self) -> Set[Any]:
        return set(vtx for vtx in self.__data.vertices() if self.out_degree(vtx) == 0)
