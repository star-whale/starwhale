from starwhale.api._impl.job import step, Context


class CustomPipeline:
    @step()
    def custom_ppl(self, context: Context) -> None:
        print("in step of ppl")

    @step(needs=["custom_ppl"])
    def custom_cmp(self, context: Context) -> None:
        print("in step of cmp")
