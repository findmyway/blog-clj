# blog-clj

这里记录的是我博客的后端代码，这是第二版了，第一版是刚学clojure的时候写的，各方面都写得很凌乱，如今又看了下clojure，还是打算重写下后端的逻辑，同时加入单元测试，希望这一版能够写得好看点~

## Prerequisites

首先需要安装 [Leiningen][] 2.0.0 或者更新的版本；然后还需要安装[redis][] 3.2.0或者更新的版本，部署的系统是Ubuntu 14.04。

[leiningen]: https://github.com/technomancy/leiningen
[redis]: http://redis.io/

## Introduction

src目录下文件的说明如下：

```
├── src
│   └── blog_clj
│       ├── handler.clj            ;; 用compojure处理请求
│       ├── page_generators.clj    ;; 从redis里获取数据后填充template
│       ├── parse.clj              ;; 解析markdown导出的html文件，提取meta信息
│       ├── redis_io.clj           ;; 与redis交互的一些函数接口
│       ├── schedules.clj          ;; 用于开启一些周期性的调度任务，暂时没用到，之前是规划着周期性fetch一些信息用的
│       ├── sync.clj               ;; 用于把本地html文件同步到数据库中
│       ├── upload_download.clj    ;; 处理些上传下载的任务
│       └── webhooks.clj           ;; 处理github的webhook，同步文件的更新
```

需要说明的几点：

1. 抛弃了以前直接从md文件渲染得到html文件的做法，原因很简单，clj下的markdown渲染库只找到了[markdown-clj][]这一个，但是支持的功能实在有限，作者更新得也比较慢，以我现在的水平自己重写一个还是需要花不少时间。而且本地写markdown文件现在用的是[MacDown][]，如何同步也是个挺麻烦的事，也考虑过直接与[MacDown][]交互，但没找到合适的接口，所以干脆直接解析html文件好了。
2. 模板引擎方面，采用了[Enlive][]和[Hiccup][]，优点自然是template与data分离，这样不必像在[Django][]中一样在template中嵌入数据与逻辑。但[Enlive][]似乎作者不打算维护了，后期可能需要在[Hickory][]的基础上做一些封装，替换掉[Enlive][];

[markdown-clj]: https://github.com/yogthos/markdown-clj
[MacDown]: http://macdown.uranusjr.com/
[Enlive]: https://github.com/cgrand/enlive
[Hiccup]: https://github.com/weavejester/hiccup
[Django]: https://www.djangoproject.com/
[Hickory]: https://github.com/davidsantiago/hickory

## Deploy

首先，需要配置几个环境变量：

```bash
export qiniu_sk="XXX"
export qiniu_ak="XXX"
export html_path="/path/to/blog-clj/resources/published-html/"
export upload_path="http://your-qiniu.qiniudn.com/upload/"

# gitub仓库对应的webhook密码
export github_webhook_secret="XXX" 
```    

然后，本地开发调试直接在根目录下运行``lein ring server``，修改代码后，server会自动重新reload对应的命名空间。

部署的话，需要先编译``lein ring uberjar``，采用Supervisor部署，一方面是因为Ubuntu下的Upstart配置了老半天，环境变量就是传不进去，很无奈；另一方面采用Supervisor可以让本地的(Mac)环境和线上的环境尽量一致，方便调试。

1. 新建deploy用户, ``sudo adduser -m deploy && sudo passwd -l deploy``
2. 将代码放在deploy用户的目录下
3. 安装并运行Supervisor
4. 在``/etc/supervisor/conf.d/blog.conf``写入以下内容，然后``supervisorctl reread && supervisorctl update && supervisorctl start blog``即可。

```
[program:blog]
environment=qiniu_sk="XXX",qiniu_ak="XXX",html_path="/path/to/blog-clj/resources/published-html/",upload_path="/path/to/blog-clj/resources/published-html/",github_webhook_secret="XXX"
command= java -jar target/blog-clj-0.1.0-SNAPSHOT-standalone.jar
directory=/path/to/blog-clj
autostart=true
autorestart=true
startretries=3
user=deploy
```


## TODO

- [ ] log日志重定向到文件
- [ ] 增加对错误异常的处理
- [ ] eastwood 静态检查

## Resources

- [Easily Deploy Your Clojure Web Site](http://www.braveclojure.com/quests/deploy/)

## License

```
MIT License

Copyright (c) 2016 Tian Jun

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
