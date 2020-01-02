# CNKI文档检索爬虫
* [初级检索](http://www.cnki.net/)
* [高级检索](http://kns.cnki.net/kns/brief/result.aspx?dbprefix=SCDB&crossDbcodes=CJFQ,CDFD,CMFD,CPFD,IPFD,CCND,CCJD)
---
## 技术栈
- Dubbo + Nacos/Zookeeper + Selenium + HttpClient + Jsoup
---
## Note
- 使用前更改注册中心及地址。注册中心默认Nacos，可更改为Zookeeper，远程调用接口:`com.csdc.spider.api.SearchService`
- 需要安装[ChromeDriver](http://chromedriver.chromium.org/downloads)并修改`com.csdc.spider.util.ConfigConsts`中的`CHROME_DRIVER_LOCATION`为软件的安装路径

