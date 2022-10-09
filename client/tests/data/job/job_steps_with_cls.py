from starwhale.api._impl.job import step, Context


class CustomPipeline:
    @step(resources={"cpu": 1})
    def custom_ppl(self, context: Context) -> None:
        print("in step of ppl")

    @step(needs=["custom_ppl"], resources={"cpu": 1})
    def custom_cmp(self, context: Context) -> None:
        print("in step of cmp")
