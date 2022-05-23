from starwhale.utils import validate_obj_name


def test_valid_object_name():
    assert validate_obj_name("a")[0]
    assert not validate_obj_name("1")[0]
    assert validate_obj_name("abc")[0]
    assert not validate_obj_name("a" * 81)[0]
    assert validate_obj_name("_adtest")[0]
    assert not validate_obj_name("_.adtest")[0]
