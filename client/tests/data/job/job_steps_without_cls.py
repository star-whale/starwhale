from starwhale.api._impl.job import step, Context


@step()
def custom_ppl(context: Context) -> None:
    print("in step of ppl")


@step(needs=["custom_ppl"])
def custom_cmp(context: Context) -> None:
    print("in step of cmp")
