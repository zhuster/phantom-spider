package com.csdc.spider.component;

import com.csdc.spider.model.Entry;
import com.csdc.spider.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

/**
 * 利用fork/join提升提取单页面上文章信息的效率
 *
 * @author zhangzhi
 * @since <pre>2019/6/9</pre>
 */
@Deprecated
@Slf4j
public class ExtractTask extends RecursiveTask<List<Entry>> {

    @Autowired
    CommonService commonService;

    private List<WebElement> trs;
    private WebDriver driver;

    public ExtractTask(List<WebElement> trs, WebDriver driver) {
        this.trs = trs;
        this.driver = driver;
    }

    @Override
    protected List<Entry> compute() {
        List<Entry> entries = new ArrayList<>();
        int size = trs.size();
        if (size < 10) {
            trs.forEach(tr -> {
                List<WebElement> tds = tr.findElements(By.tagName("td"));
                Entry entry;
                try {
                    entry = commonService.extractPaperEntry(tds, driver);
                } catch (Exception e) {
                    log.error("提取文章信息失败");
                    throw e;
                }
                entries.add(entry);
            });
        } else {
            int pivot = trs.size() / 2;
            ExtractTask leftTask = new ExtractTask(trs.subList(0, pivot), driver);
            ExtractTask rightTask = new ExtractTask(trs.subList(pivot + 1, size - 1), driver);
            leftTask.fork();
            rightTask.fork();
            List<Entry> leftEntries = leftTask.join();
            List<Entry> rightEntries = rightTask.join();
            entries.addAll(leftEntries);
            entries.addAll(rightEntries);
        }
        return entries;
    }
}
