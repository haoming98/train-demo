# train-demo

## 镜像制作
### 前期准备
1. 拉取版本
```bash
git clone https://github.com/nickilchen/train-demo.git
```
2. 根据系统配置好jdk、maven、nodejs环境
3. 在docker hub上注册一个属于自己的账号

### jib制作springboot镜像
> 以eurekaserver、service-hello为例

#### 制作eurekaserver镜像
1. 编辑pom.xml添加jib插件
```xml
<!-- 谷歌·jib 镜像打包插件 -->
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>2.4.0</version>
    <configuration>
        <from>
            <!-- 用于打包的基础镜像 -->
            <image>registry.cn-shanghai.aliyuncs.com/gvsun/jdk:8u141</image>
        </from>
        <to>
            <image>192.168.1.134:443/lubanlou/${project.name}:${project.version}</image>
        </to>
        <!--容器相关的属性-->
        <container>
            <!-- 创建时间 -->
            <creationTime>USE_CURRENT_TIMESTAMP</creationTime>
            <!-- spring boot项目配置主类 -->
            <mainClass>com.ocly.EurekaserverApplication</mainClass>
        </container>
        <!--允许非https-->
        <allowInsecureRegistries>true</allowInsecureRegistries>
    </configuration>
</plugin>
```
2. mvn 编译上传镜像

```shell
#编译项目
# maven.clean.failOnError=false：强制删除target
# maven.test.skip=true： 跳过测试
# maven.compile.fork=true 开启多线程编译
# -U: 刷新依赖包
mvn -T 2C clean compile \
-Dmaven.clean.failOnError=false -Dmaven.test.skip=true -U \
-Dmaven.compile.fork=true

#jib打包镜像并上传镜像
# jib.to.image：上传镜像到仓库以及镜像名
# jib.to.auth.username：镜像库用户名
# jib.to.auth.password： 镜像库用户密码
mvn jib:build \
-Djib.to.image=nickilchen/eurekaserver:v_1
```

#### 制作service-hello镜像
1. 修改application.yml中的参数为环境变量  
第一处
```yaml
defaultZone: http://localhost:8761/eureka/
```
修改为
```yaml
defaultZone: ${eureka_host:http://localhost:8761/eureka/}
```
第二处
```yaml
message:
  name: nickchen
```
修改为
```yaml
message:
  name: ${message_name:nickchen}
```
2. 重复制作eurekaserver镜像的操作

### Dockerfile 制作xcx 后台镜像
1. 在xcx目录下新建xcx_pm2.json
```json
{
    "apps": [{
        "name": "xcx",
        "script": "bin/www",
        "watch": [
            "routes"
        ],
        "max_memory_restart": "100M",
        "instances"  : 4,
        "exec_mode"  : "cluster",
        "env" : {
           "NODE_ENV": "production"
         }
    }]
}
```
2. 在xcx目录下新建Dokcerfile
```Dockerfile
FROM registry.cn-hangzhou.aliyuncs.com/gvsun/pm2:8-alpine
WORKDIR /opt/web
RUN mkdir -p /opt/web
COPY ./ /opt/web
ENV NPM_CONFIG_LOGLEVEL warn
RUN npm install --registry=https://registry.npm.taobao.org
RUN ls -al -R
CMD [ "pm2-runtime", "start", "xcx_pm2.json" ]
```
3. 构建xcx后台镜像
```bash
 docker build -t nickilchen/xcx:v_1 .
 docker push nickilchen/xcx:v_1
```


### Dockerfile制作lbl-front 前端镜像

1. 在 lbl-front目录下新建nginx配置文件（nginx.conf）
 ```conf
# 配置用户或者组，默认为nobody nobody,镜像里为nginx用户。
user  nginx;
# 一般最多是8个，多于8个基本性能不会相应的提升。设置一般是cpu的个数
worker_processes  4;
# 这个指令是指当一个nginx进程打开的最多文件描述符数目，理论值要和系统的单进程打开文件数一致，即可设置为系统优化后的ulimit -HSn的结果
worker_rlimit_nofile 65535;
# 错误日志
error_log  /var/log/nginx/error.log warn;
# 进程pid存放位置
pid        /var/run/nginx.pid;
# cpu亲和力配置，让不同的进程使用不同的cpu
#worker_cpu_affinity 0001 0010 0100 1000;
# 事件模块
events {
    ###################事件驱动配置########################
    # Select、poll属于标准事件模型，如果当前系统不存在更有效的方法，nginx会选择select或poll
    # Kqueue：使用于FreeBSD 4.1+, OpenBSD 2.9+, NetBSD 2.0 和 MacOS X.使用双处理器的MacOS X系统使用kqueue可能会造成内核崩溃。
    # Epoll:使用于Linux内核2.6版本及以后的系统。
    # /dev/poll：使用于Solaris 7 11/99+, HP/UX 11.22+ (eventport), IRIX 6.5.15+ 和 Tru64 UNIX 5.1A+。
    # Eventport：使用于Solaris 10. 为了防止出现内核崩溃的问题,有必要安装安全补丁。
    ######################################################
    use epoll;
    # 设置一个进程是否同时接受多个网络连接，默认为off
    multi_accept on;
    #设置网路连接序列化，防止惊群现象发生，默认为on
    accept_mutex on;
    #工作进程的最大连接数量 理论上每台nginx服务器的最大连接数为worker_processes*worker_connections worker_processes为我们再main中开启的进程数
    worker_connections  65535;
    # 这个将为打开文件指定缓存，默认是没有启用的，max指定缓存数量，建议和打开文件数一致，inactive是指经过多长时间文件没被请求后删除缓存。
    # open_file_cache max=65535 inactive=60s;
    # 这个是指多长时间检查一次缓存的有
    # open_file_cache指令中的inactive参数时间内文件的最少使用次数，如果超过这个数字，文件描述符一直是在缓存中打开的，如上例，如果有一个文件在inactive时间内一次没被使用，它将被移除。
    # open_file_cache_min_uses 1;
}
http {
    # 文件扩展名与文件类型映射表
    include       /etc/nginx/mime.types;
    # 默认文件类型，默认为text/plain
    default_type  application/octet-stream;

    ################# limit模块，可防范一定量的DDOS攻击 #####################
    # 用来存储session会话的状态，如下是为session分配一个名为one的10M的内存存储区，限制了每秒只接受一个ip的一次请求 1r/s
    #  limit_req_zone $binary_remote_addr zone=one:10m rate=1r/s;
    #  limit_conn_zone $binary_remote_addr zone=addr:10m;
    #  include       mime.types;
    #  default_type  application/octet-stream;
    ########################################################################

    ############# SSI配置，SSI是一种基于服务端的网页制作技术 ##################
    # ssi on;
    # ssi_silent_errors on;
    # ssi_types text/shtml;
    # include       mime.types;
    # default_type  application/octet-stream;
    ########################################################################

    ####################### 自定义日志格式 ##################################
    #  $remote_addr与$http_x_forwarded_for用以记录客户端的ip地址；
    #  $remote_user：用来记录客户端用户名称；
    #  $time_local： 用来记录访问时间与时区；
    #  $request： 用来记录请求的url与http协议；
    #  $status： 用来记录请求状态；成功是200，
    #  $body_bytes_sent ：记录发送给客户端文件主体内容大小；
    #  $http_referer：用来记录从那个页面链接访问过来的；
    #  $http_user_agent：记录客户浏览器的相关信息；
    #  通常web服务器放在反向代理的后面，这样就不能获取到客户的IP地址了，通过$remote_add拿到的IP地址是反向代理服务器的iP地址。反向代理服务器在转发请求的http头信息中，可以增加x_forwarded_for信息，用以记录原有客户端的IP地址和原来客户端的请求的服务器地址。
    ########################################################################
    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    # combined为日志格式的默认值
    access_log  /var/log/nginx/access.log  main;

    # 隐藏nginx版本
    server_tokens off;

    ######################################################################
    # 允许sendfile方式传输文件，默认为off，可以在http块，server块，location块。
    sendfile        on;
    # 每个进程每次调用传输数量不能大于设定的值，默认为0，即不设上限。
    # sendfile_max_chunk 100k;
    # 在nginx中，tcp_nopush配置与tcp_nodelay“互斥”。它可以配置一次发送数据包的大小。也就是说，数据包累积到一定大小后就发送。在nginx中tcp_nopush必须和sendfile配合使用。可以在http块，server块，location块。
    tcp_nopush     on;
    # 告诉nginx不要缓存数据，而是一段一段的发送--当需要及时发送数据时，就应该给应用设置这个属性，这样发送一小块数据信息时就不能立即得到返回值。可以在http块，server块，location块。
    # tcp_nodelay    on;
    #####################################################################

    ####################### 请求体相关参设置 #############################
    # 用来设置允许客户端请求的最大的单个文件字节数
    client_max_body_size 1000M;
    # 为存储承载客户端请求正文的临时文件定义存储目录。在指定目录下可支持高达3层子目录结构。例如，下面的配置：
    # client_body_temp_path /spool/nginx/client_temp 1 2;
    client_body_temp_path /tmp;
    # 设置客户端请求头读取超时时间。如果超过这个时间，客户端还没有发送任何数据，Nginx将返回“Request time out（408）”错误
    client_header_timeout 300s;
    # 设置客户端请求主体读取超时时间。如果超过这个时间，客户端还没有发送任何数据，Nginx将返回“Request time out（408）”错误，默认值是60。
    client_body_timeout 300s;
    client_body_buffer_size 1000m;
    # 客户请求头缓冲大小。nginx默认会用client_header_buffer_size这个buffer来读取header值，如果header过大，它会使用large_client_header_buffers来读取。
    large_client_header_buffers 8 256k;
    # 客户端请求头部的缓冲区大小。这个可以根据你的系统分页大小来设置，一般一个请求头的大小不会超过1k，不过由于一般系统分页都要大于1k，所以这里设置为分页大小。分页大小可以用命令getconf PAGESIZE 取得.当要client_header_buffer_size超过4k的情况，但是client_header_buffer_size该值必须设置为“系统分页大小”的整倍数。
    client_header_buffer_size 4k;
    ####################################################################

    # 连接超时时间，默认为75s，可以在http，server，location块。
    keepalive_timeout  90s;
    # 单连接请求数上限，该指令用于限制用户通过某一个连接向Nginx服务器发起请求的次数
    keepalive_requests 50000;

    ######################## gzip 压缩配置 #############################
    # gzip 是告诉nginx采用gzip压缩的形式发送数据。这将会减少我们发送的数据量。
    gzip on;
    # 设置允许压缩的页面最小字节数，页面字节数从header头的Content-Length中获取。默认值是0，表示不管页面多大都进行压缩。建议设置成大于1K。如果小于1K可能会越压越大。
    gzip_disable "msie6";
    gzip_min_length 0;
    # 压缩缓冲区大小。表示申请4个单位为16K的内存作为压缩结果流缓存，默认值是申请与原始数据大小相同的内存空间来存储gzip压缩结果。
    gzip_buffers 16 8K;
    # 压缩版本（默认1.1，前端为squid2.5时使用1.0）用于设置识别HTTP协议版本，默认是1.1，目前大部分浏览器已经支持GZIP解压，使用默认即可。
    gzip_http_version 1.1;
    # 压缩比率。用来指定GZIP压缩比，1压缩比最小，处理速度最快；9压缩比最大，传输速度快，但处理最慢，也比较消耗cpu资源。
    gzip_comp_level 8;
    # 用来指定压缩的类型，“text/html”类型总是会被压缩
    gzip_types
        text/xml application/xml application/atom+xml application/rss+xml application/xhtml+xml image/svg+xml application/font-woff
        text/javascript application/javascript application/x-javascript
        text/x-json application/json application/x-web-app-manifest+json
        text/css text/plain text/x-component
        font/opentype application/x-font-ttf application/vnd.ms-fontobject font/woff2
        image/x-icon image/png image/jpeg image/gif;
    # vary header支持。该选项可以让前端的缓存服务器缓存经过GZIP压缩的页面，例如用Squid缓存经过Nginx压缩的数据。
    gzip_vary on;
    gzip_static on;
    gzip_proxied any;
    # 检查预压缩文件
    # gunzip_static on;
    # IE6以下的浏览器不启用gzip,有些较老的浏览不能解压数据
    gzip_disable "MSIE [1-6]\.(?!.*SV1)";
    ####################################################################

    # 修改编码为UTF-8。让有中文的url得以支持
    charset utf-8;
    # 引入相关配置文件
    #include /etc/nginx/conf.d/*.conf;
    server {
       listen       80;
        location / {
           root /web/;
           try_files $uri $uri/ /index.html last;
           index index.html;
        }
    }
}
 ```
2. 在lbl-front目录下新建Dockerfile

```Dockerfile
FROM nginx:1.19.1-alpine-perl

COPY nginx.conf /etc/nginx/
COPY dist/ /web/

CMD ["nginx", "-g", "daemon off;"]
```
3. docker 命令构建镜像
```bash
#生成打包文件
npm install
npm run build
#docker build -t 你的仓库 .
docker build -t nickilchen/lbl-front:v_1 .
#上传镜像
docker push nickilchen/lbl-front:v_1
```
## k8s 启动
