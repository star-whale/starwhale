*** Settings ***
Documentation     This is a resource file, that can contain variables and keywords.
...               Keywords defined here can be used where this Keywords.resource in loaded.
Library           cmds/instance_cmd.py
Library           cmds/project_cmd.py
Library           cmds/model_cmd.py
Library           cmds/dataset_cmd.py
Library           cmds/runtime_cmd.py
Library           cmds/eval_cmd.py


*** Variables ***
${FULLNAME}     Administrator    #FULLNAME is a scalar variable containing the string 'Administrator'
@{ALL USERS}    admin    hulk    dr.strange    captain    ironman    #List variable with 5 values.
&{ADMIN}        login=admin    pwd=@RBTFRMWRK@    #Dictionary variable
&{SPIDERMAN}    login=spider    name=Peter Parker    right=user    active=${True}


*** Keywords ***
Connect to Server
    Connect    fe80::aede:48ff:fe00:1122

Close Server Connection
    Disconnect

Login User
    [Arguments]    ${login}    ${password}
    Set Login Name    ${login}
    Set Password    ${password}
    Execute Login

Verify Valid Login
    [Arguments]    ${exp_full_name}
    ${version}=    Get Server Version
    Should Not Be Empty    ${version}
    ${name}=    Get User Name
    Should Be Equal    ${name}    ${exp_full_name}

Login Admin
    [Documentation]    'Login Admin' is a Keyword.
    ...                It calls 'Login User' from 'CustomLibrary.py'
    Login User    ${ADMIN}[login]    ${ADMIN}[pwd]
    Verify Valid Login    Administrator

Verify User Details
    [Documentation]    Keyword that fetches user details based on a login
    ...                and verifies them against expected arguments.
    [Arguments]   ${login}   ${name}   ${right}=user   ${active}=True
    ${user_id}=    Get User Id    ${login}
    ${user_details}=    Get User Details    ${user_id}
    Should Be Equal               ${user_details}[login]    ${login}
    Should Be Equal               ${user_details}[name]     ${name}
    Should Be Equal               ${user_details}[right]    ${right}
    Should Be Equal As Strings    ${user_details}[active]   ${active}

Print All Users
    ${users}=    Get All Users
    FOR    ${user}    IN    @{users}
        Log    ${user}
    END

Get User Details By Name
    [Arguments]    ${login}=${None}
    IF    $login
        ${user_id}=    Get User Id    ${login}
    ELSE
        ${user_id}=    Set Variable    ${NONE}
    END
    ${user_details}=    Get User Details    ${user_id}
    Log    ${user_details}
    [Return]    ${user_details}
