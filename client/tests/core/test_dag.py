import copy
from unittest import TestCase

from starwhale.base.scheduler.dag import DAG


class DagTest(TestCase):
    def test_dag(self):
        dag = DAG()
        dag.add_vertex("init", "ppl", "cmp", "ppl-1", "cmp-1", "end")
        dag.add_edge("init", "ppl")
        dag.add_edge("ppl", "cmp")
        dag.add_edge("cmp", "end")
        dag.add_edge("init", "ppl-1")
        dag.add_edge("ppl-1", "cmp-1")
        dag.add_edge("cmp-1", "end")

        assert dag.vertex_size() == 6
        assert dag.edge_size() == 6

        with self.assertRaises(RuntimeError):
            dag.add_edge("init-x", "end")

        # test remove
        dag_cp = copy.deepcopy(dag)
        with self.assertRaises(RuntimeError):
            dag_cp.remove_edge("ppl-1", "cmp")

        # not found
        with self.assertRaises(RuntimeError):
            dag_cp.remove_edge("ppl-1", "cmp-2")

        dag_cp.remove_edge("ppl", "cmp")
        dag_cp.remove_edge("cmp", "end")

        with self.assertRaises(RuntimeError):
            dag.add_edge("end", "init")

        assert dag.successors("init") == {"ppl", "ppl-1"}

        assert dag.predecessors("ppl") == {"init"}
        assert dag.predecessors("cmp") == {"ppl"}
        assert dag.predecessors("end") == {"cmp", "cmp-1"}
        assert dag.all_starts() == {"init"}
