#知网文章检索爬虫
* [初级检索](http://www.cnki.net/)
* [高级检索](http://kns.cnki.net/kns/brief/result.aspx?dbprefix=SCDB&crossDbcodes=CJFQ,CDFD,CMFD,CPFD,IPFD,CCND,CCJD)
---
##技术栈
- Dubbo + Zookeeper + Selenium + HttpClient + Jsoup
---
- Note:使用前更改注册中心地址，远程调用接口:`com.csdc.spider.api.SearchService`