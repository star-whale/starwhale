*** Settings ***
Documentation     A test suite for valid login.
...
...               Keywords are imported from the resource file
Resource          keywords.resource
Suite Setup       Connect to Server
Test Teardown     Logout User
Suite Teardown    Disconnect

*** Test Cases ***
Access All Users With Admin Rights
    [Documentation]    Tests if all users can be accessed with Admin User.
    Login Admin
    Print All Users

Create User With Admin Rights
    [Documentation]    Tests if a new users can be created with Admin User.
    Login Admin
    Create New User
    ...    name=Peter Parker
    ...    login=spider
    ...    password=123spiderman321
    ...    right=user
    Verify User Details    spider    Peter Parker
    Logout User
    Login User    spider    123spiderman321


Update User with Admin Rights
    [Documentation]    Changes Password of an existing user.
    Login Admin
    Change Users Password    spider    friendly_spider_2022
    Logout User
    Login User    spider    friendly_spider_2022

Update Own Password With User Rights
    [Documentation]    Changes Password of an existing user.
    Login User    hulk    Hulk...SMASH!
    Change Own Password    Don't make Hulk angry!    Hulk...SMASH!
    Logout User
    Login User    hulk    Don't make Hulk angry!

Access Own Details With User Rights
    [Documentation]    Tests if a user can access own details
    Login User    ironman    1234567890
    Get User Details By Name    ironman
    Get User Details By Name
    Get User Details
    Verify User Details    ironman    Tony Stark

Access Other Users Details With User Rights
    [Documentation]    Tests does fail, due to insufficiant rights...
    [Setup]    Login User     ironman    1234567890
    Get User Details By Name    ironman
    Get User Details By Name    hulk
    Log     This keyword not be executed
    [Teardown]    Log    This is executed.
