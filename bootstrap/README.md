- use origin cmd
  - first, install ansible: `pip install ansible`
  - then,
    - (**important**) make sure this machine can access all hosts bellow with the user of ${sudoer} !! 
    - edit two hosts file at your machine,
      - one is /etc/hosts,here is a content example:
        ``` text
         10.131.0.1 agent01.starwhale.com
         10.131.0.2 agent02.starwhale.com storage.starwhale.com controller.starwhale.com
         10.131.0.3 agent03.starwhale.com
         10.131.0.4 agent04.starwhale.com
        ``` 
      - another one is $PWD/hosts,here is a content example:
        ``` text
         [controller]
         controller.starwhale.com 
         [storage] 
         storage.starwhale.com
         [agent]
         agent01.starwhale.com
         agent02.starwhale.com
         agent03.starwhale.com
         agent04.starwhale.com
         ...
        ```
  - then, exec: `ansible-playbook bootstrap.yaml -i hosts --user ${sudoer} --check`
  - finally, exec: `ansible-playbook bootstrap.yaml -i hosts --user {sudoer}`
- use make cmd
  - `make install`
  - edit the hosts file at your machine,the same with above example
  - `make check {sudoer}`
  - `make play {sudoer}`