from starwhale.api._impl.job import step, Context


@step(resources={"cpu": 1})
def custom_ppl(context: Context) -> None:
    print("in step of ppl")


@step(needs=["custom_ppl"], resources={"cpu": 1})
def custom_cmp(context: Context) -> None:
    print("in step of cmp")
