- use origin cmd
  - first, install bootstrap: `pip install starwhale-bootstarp`
  - second,(**important**) make sure this machine can access all hosts bellow with the user of ${sudoer} !! 
  - then, edit hosts file(/etc/hosts) at your machine,here is a content example:
      ``` text
       10.131.0.1 agent01.starwhale.com
       10.131.0.2 agent02.starwhale.com storage.starwhale.com controller.starwhale.com
       10.131.0.3 agent03.starwhale.com nexus.starwhale.com
       10.131.0.4 agent04.starwhale.com
      ```
  - finally, exec: `starwhale-bootstrap deploy run --user ${sudoer} --hosts-of-agent "agent01.starwhale.com,agent02.starwhale.com,agent03.starwhale.com,..."`
    - the default host of controller and storage is 'controller.starwhale.com' and 'storage.starwhale.com', So you don't have to pass in