主要目标：测试基于jwt的完整流程：

- 用户登录：
    - 正确用户名：正常，且response中带header=Authorization
    - 错误用户名；状态码 401
- 普通接口访问
    - 带token访问：
        - token有效期内
            - 访问有权限接口，200 正常返回
            - 访问无权限接口，403 返回access denied异常
        - token失效，？？？ 返回异常
    - 不带token访问，？？？ 返回unauthorized异常
