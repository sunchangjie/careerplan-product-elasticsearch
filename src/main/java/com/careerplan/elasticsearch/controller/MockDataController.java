package com.careerplan.elasticsearch.controller;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.careerplan.elasticsearch.common.core.JsonResult;
import com.careerplan.elasticsearch.dto.MockData1Dto;
import com.careerplan.elasticsearch.dto.MockData2Dto;
import com.careerplan.elasticsearch.dto.MockData3Dto;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 造商品数据的控制类
 *
 * @author zhonghuashishan
 */
@Slf4j
@RestController
@RequestMapping("/api/mockData")
public class MockDataController {

    private static final String dataFileName = "100k_products.txt";

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 单线程插入模拟的商品数据
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.9/java-rest-high-document-bulk.html
     */
    @PostMapping("/mockData1")
    public JsonResult mockData1(@RequestBody MockData1Dto request) throws IOException {
        if (!request.validateParams()) {
            return JsonResult.buildError("参数有误");
        }

        String indexName = request.getIndexName();
        int batchTimes = request.getBatchTimes();
        int batchSize = request.getBatchSize();

        // 1、从txt文件里面加载10w条商品数据
        List<Map<String, Object>> skuList = loadSkusFromTxt();

        long startTime = System.currentTimeMillis();

        // 真正在生产环境下，不可能说单个线程大批量数据，一个一个batch做导入，这个方法实现主要是用来跟
        // 多线程批量导入的方法做一个性能对比

        // 2、每次随机取出batchSize个商品数据，然后批量插入，一共执行batchTimes次
        for (int i = 0; i < batchTimes; i++) {
            BulkRequest bulkRequest = buildSkuBulkRequest(indexName, batchSize, skuList);
            restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT); // 把你指定的多条数据打包成一个bulk，插入到es里去
            log.info("插入[{}]条商品数据", batchSize);
        }

        long endTime = System.currentTimeMillis();

        // 3、记录统计信息
        int totalCount = batchSize * batchTimes;
        long elapsedSeconds = (endTime - startTime) / 1000;
        long perSecond = totalCount / elapsedSeconds;
        log.info("此次共导入[{}]条商品数据，耗时[{}]秒，平均每秒导入[{}]条数据", totalCount, elapsedSeconds, perSecond);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startTime", DateUtil.format(new Date(startTime), DatePattern.NORM_DATETIME_PATTERN));
        result.put("endTime", DateUtil.format(new Date(endTime), DatePattern.NORM_DATETIME_PATTERN));
        result.put("totalCount", totalCount);
        result.put("elapsedSeconds", elapsedSeconds);
        result.put("perSecond", perSecond);
        return JsonResult.buildSuccess(result);
    }

    /**
     * 多线程插入模拟的商品数据
     */
    @PostMapping("/mockData2")
    public JsonResult mockData2(@RequestBody MockData2Dto request) throws IOException, InterruptedException {
        if (!request.validateParams()) {
            return JsonResult.buildError("参数有误");
        }

        String indexName = request.getIndexName();
        int batchTimes = request.getBatchTimes();
        int batchSize = request.getBatchSize();
        int threadCount = request.getThreadCount();
        List<Map<String, Object>> skuList = loadSkusFromTxt();

        CountDownLatch countDownLatch = new CountDownLatch(batchTimes); // 倒计数 -> 一个一个线程完成之后，countDown，所有线程都完成了以后，才算结束
        Semaphore semaphore = new Semaphore(threadCount);

        // 信号量，一个线程可以尝试从semaphore获取一个信号，如果获取不到就阻塞等待，获取到了，信号就是你的了
        // 用完了这个信号之后，可以把信号还回去，最多就只能有threadCount个线程，去获取到信号量

        // 虽然semaphore可以控制了同时进行的任务数，但是maximumPoolSize也不能设置的和semophore一样的大小，
        // 因为用的是SynchronousQueue这个队列，可能会出现需要比semaphore多需要一两个线程的情况，实际线程数不会达到threadCount * 2
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount * 2,
                60, TimeUnit.SECONDS, new SynchronousQueue<>());

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < batchTimes; i++) { // batch数量可以是比线程数量是要多的
            // 保证一直有threadCount个线程同时在执行批量插入的操作
            // 先获取一个信号量，获取到了就执行批量插入的操作，获取不到就在这里等着有空余的信号量
            // 如果说threadCount个线程数量对应的semaphore信号量耗尽了以后
            semaphore.acquireUninterruptibly();

            threadPoolExecutor.submit(() -> {
                try {
                    BulkRequest bulkRequest = buildSkuBulkRequest(indexName, batchSize, skuList);
                    restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                    // BulkRequest bulkRequest = buildSuggestIndexBulkRequest(xx);
                    // restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                    log.info("线程[{}]插入[{}]条商品数据", Thread.currentThread().getName(), batchSize);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                    countDownLatch.countDown();
                }
            });
        }

        long endTime = System.currentTimeMillis();

        // 在这里等待一下最后一个批次的批量插入操作执行完
        countDownLatch.await();

        // 现在的使用方式，在这里需要手动的把线程池给关掉
        threadPoolExecutor.shutdown();

        int totalCount = batchSize * batchTimes;
        long elapsedSeconds = (endTime - startTime) / 1000;
        long perSecond = totalCount / elapsedSeconds;
        log.info("此次共导入[{}]条商品数据，耗时[{}]秒，平均每秒导入[{}]条数据", totalCount, elapsedSeconds, perSecond);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startTime", DateUtil.format(new Date(startTime), DatePattern.NORM_DATETIME_PATTERN));
        result.put("endTime", DateUtil.format(new Date(endTime), DatePattern.NORM_DATETIME_PATTERN));
        result.put("totalCount", totalCount);
        result.put("elapsedSeconds", elapsedSeconds);
        result.put("perSecond", perSecond);
        return JsonResult.buildSuccess(result);
    }

    /**
     * 多线程插入模拟的商品数据
     */
    @PostMapping("/indexAllProductData")
    public JsonResult indexAllProductData(@RequestBody MockData2Dto request) throws IOException, InterruptedException {
        if (!request.validateParams()) {
            return JsonResult.buildError("参数有误");
        }

        int batchCount = 1;
        int batchSize = 100_000;
        int bulkSize = request.getBatchSize();
        int bulkCount = batchSize / bulkSize + 1; // 100000 / 150 = 666，667可以是有一点点收尾的数据做处理，完全没数据做一个处理
        int threadCount = request.getThreadCount();

        long startTime = System.currentTimeMillis();

        for(int batchIndex = 1; batchIndex <= batchCount; batchIndex++) {
            // 这个通常来说就是10w10w的数据一批一批的查
            // 查出来的每一批数据，都是要拆分为多个bulk进行并发批量插入
            List<Map<String, Object>> batchList = queryProductBatchFromDatabase(batchIndex, batchSize);

            CountDownLatch countDownLatch = new CountDownLatch(bulkCount);
            Semaphore semaphore = new Semaphore(threadCount);

            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                    threadCount,
                    threadCount * 2,
                    60,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>());

            int bulkDataCurrentIndex = 0;

            for (int bulkIndex = 1; bulkIndex <= bulkCount; bulkIndex++) {
                List<Map<String, Object>> bulkList = new ArrayList<Map<String, Object>>();
                List<String> skuNameBulkList = new ArrayList<String>();

                for(int bulkDataIndex = bulkDataCurrentIndex; bulkDataIndex < bulkDataCurrentIndex + bulkSize; bulkDataIndex++) {
                    if(batchList.get(bulkDataIndex) == null) {
                        break;
                    }
                    bulkList.add(batchList.get(bulkDataIndex));
                    skuNameBulkList.add(String.valueOf(batchList.get(bulkDataIndex).get("skuName")));
                }

                bulkDataCurrentIndex += bulkSize;

                semaphore.acquireUninterruptibly();

                threadPoolExecutor.submit(() -> {
                    try {
                        if(bulkList.size() > 0) {
                            BulkRequest productIndexBulkRequest = buildProductIndexBulkRequest(bulkList);
                            restHighLevelClient.bulk(productIndexBulkRequest, RequestOptions.DEFAULT);
                        }
                        if(skuNameBulkList.size() > 0) {
                            BulkRequest suggestIndexBulkRequest = buildSuggestIndexBulkRequest(skuNameBulkList);
                            restHighLevelClient.bulk(suggestIndexBulkRequest, RequestOptions.DEFAULT);
                        }
                        log.info("线程[{}]插入[{}]条商品数据", Thread.currentThread().getName(), bulkList.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        semaphore.release();
                        countDownLatch.countDown();
                    }
                });
            }

            countDownLatch.await();
            threadPoolExecutor.shutdown();
        }

        long endTime = System.currentTimeMillis();

        int totalCount = batchSize * batchCount;
        long elapsedSeconds = (endTime - startTime) / 1000;
        long perSecond = totalCount / elapsedSeconds;
        log.info("此次共导入[{}]条商品数据，耗时[{}]秒，平均每秒导入[{}]条数据", totalCount, elapsedSeconds, perSecond);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startTime", DateUtil.format(new Date(startTime), DatePattern.NORM_DATETIME_PATTERN));
        result.put("endTime", DateUtil.format(new Date(endTime), DatePattern.NORM_DATETIME_PATTERN));
        result.put("totalCount", totalCount);
        result.put("elapsedSeconds", elapsedSeconds);
        result.put("perSecond", perSecond);
        return JsonResult.buildSuccess(result);
    }

    private List<Map<String, Object>> queryProductBatchFromDatabase(int batchIndex, int batchSize) {
        // 根据你是第几个batch，每个batch多少条数据，从数据库里去做一个sql查询，把一批一批的数据查出来
        return new ArrayList<Map<String, Object>>();
    }

    private BulkRequest buildProductIndexBulkRequest(List<Map<String, Object>> bulkList) {
        BulkRequest bulkRequest = new BulkRequest("product_index");
        for (int i = 0; i < bulkList.size(); i++) {
            Map<String, Object> productDataMap = bulkList.get(i);

            List<Object> productDataList = new ArrayList<>();
            productDataMap.forEach((k, v) -> {
                productDataList.add(k);
                productDataList.add(v);
            });

            IndexRequest indexRequest = new IndexRequest().source(XContentType.JSON, productDataList.toArray());
            bulkRequest.add(indexRequest);
        }
        return bulkRequest;
    }

    private BulkRequest buildSuggestIndexBulkRequest(List<String> skuNameBulkList) throws Exception {
        BulkRequest bulkRequest = new BulkRequest("suggest_index");
        for (int i = 0; i < skuNameBulkList.size(); i++) {
            String skuName = skuNameBulkList.get(i);
            IndexRequest indexRequest = new IndexRequest().source(XContentType.JSON, "word1", skuName, "word2", skuName);
            bulkRequest.add(indexRequest);
        }
        return bulkRequest;
    }

    /**
     * 单线程插入模拟的suggest数据
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.9/java-rest-high-document-bulk.html
     */
    @PostMapping("/mockData3")
    public JsonResult mockData3(@RequestBody MockData3Dto request) throws IOException {
        if (!request.validateParams()) {
            return JsonResult.buildError("参数有误");
        }

        String indexName = request.getIndexName();
        int batchTimes = request.getBatchTimes();
        int batchSize = request.getBatchSize();

        // 1、从txt文件里面加载10w个商品名称
        List<String> skuNameList = loadSkuNamesFromTxt();

        long startTime = System.currentTimeMillis();

        // 2、从第1条数据开始导入
        int index = 0;
        for (int i = 0; i < batchTimes; i++) {
            BulkRequest bulkRequest = new BulkRequest(indexName);
            for (int j = 1; j <= batchSize; j++) {
                String skuName = skuNameList.get(index);
                IndexRequest indexRequest = new IndexRequest().source(XContentType.JSON, "word1", skuName, "word2", skuName);
                System.out.println(skuName);
                bulkRequest.add(indexRequest);
                index++;
            }
            log.info("开始插入[{}]条suggest数据", batchSize);
            restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            log.info("完成插入[{}]条suggest数据", batchSize);
        }

        long endTime = System.currentTimeMillis();

        // 3、记录统计信息
        int totalCount = batchSize * batchTimes;
        long elapsedSeconds = (endTime - startTime) / 1000;
        long perSecond = totalCount / elapsedSeconds;
        log.info("此次共导入[{}]条suggest数据，耗时[{}]秒，平均每秒导入[{}]条数据", totalCount, elapsedSeconds, perSecond);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startTime", DateUtil.format(new Date(startTime), DatePattern.NORM_DATETIME_PATTERN));
        result.put("endTime", DateUtil.format(new Date(endTime), DatePattern.NORM_DATETIME_PATTERN));
        result.put("totalCount", totalCount);
        result.put("elapsedSeconds", elapsedSeconds);
        result.put("perSecond", perSecond);
        return JsonResult.buildSuccess(result);
    }

    /**
     * 读取txt文件中的sku数据
     */
    private List<Map<String, Object>> loadSkusFromTxt() throws IOException {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(dataFileName);
        InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        List<Map<String, Object>> skuList = new ArrayList<>();

        // 读取文件内容（一共是100k条商品数据）
        // 10001,房屋卫士自流平美缝剂瓷砖地砖专用双组份真瓷胶防水填缝剂镏金色,品质建材,398.00,上海,540785126782
        String line;
        Random random = new Random();
        while ((line = bufferedReader.readLine()) != null) {
            String[] segments = line.split(",");
            int id = Integer.parseInt(segments[0]);
            String skuName = segments[1];
            String category = segments[2].replace("会场", "").replace("主会场", "").replace("风格好店", "");
            int basePrice = Integer.parseInt(segments[3].substring(0, segments[3].indexOf(".")));
            if (basePrice <= 100) {
                basePrice = 200;
            }

            // 10个字段
            Map<String, Object> sku = new HashMap<>();
            sku.put("skuId", id);
            sku.put("skuName", skuName);
            sku.put("category", category);
            sku.put("basePrice", basePrice);
            sku.put("vipPrice", basePrice - 100);

            sku.put("saleCount", random.nextInt(100_000));
            sku.put("commentCount", random.nextInt(100_000));
            sku.put("skuImgUrl", "http://sku_img_url.png");
            sku.put("createTime", "2021-01-04 10:00:00");
            sku.put("updateTime", "2021-01-04 10:00:00");
            skuList.add(sku);
        }
        return skuList;
    }

    /**
     * 从10万个sku里面随机选择batchSize个，然后封装成一个批量插入的BulkRequest对象
     */
    private BulkRequest buildSkuBulkRequest(String indexName, int batchSize, List<Map<String, Object>> skuList) {
        BulkRequest bulkRequest = new BulkRequest(indexName);
        Random random = new Random();
        for (int j = 0; j < batchSize; j++) {
            int index = random.nextInt(100_000); // 0到100000
            Map<String, Object> map = skuList.get(index);
            // skuId=xx
            // skuName=xx
            // category=xx
            // 一条数据

            // list[0] = skuId，list[1] = xx，list[2] = skuName, list[3] = xx
            List<Object> list = new ArrayList<>();
            map.forEach((k, v) -> {
                list.add(k);
                list.add(v);
            });

            IndexRequest indexRequest = new IndexRequest().source(XContentType.JSON, list.toArray());
            bulkRequest.add(indexRequest);
        }
        return bulkRequest;
    }

    /**
     * 读取txt文件中的sku名称数据
     */
    private List<String> loadSkuNamesFromTxt() throws IOException {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(dataFileName);
        InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        List<String> skuNameList = new ArrayList<>();

        // 读取文件内容（一共是100k条商品数据）
        // 10001,房屋卫士自流平美缝剂瓷砖地砖专用双组份真瓷胶防水填缝剂镏金色,品质建材,398.00,上海,540785126782
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] segments = line.split(",");
            String skuName = segments[1];
            skuNameList.add(skuName);
        }
        return skuNameList;
    }
}