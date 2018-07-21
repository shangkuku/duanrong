import com.arronlong.httpclientutil.HttpClientUtil;
import com.arronlong.httpclientutil.builder.HCB;
import com.arronlong.httpclientutil.common.HttpConfig;
import com.arronlong.httpclientutil.common.HttpHeader;
import com.arronlong.httpclientutil.common.SSLs;
import com.arronlong.httpclientutil.exception.HttpProcessException;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpCoreContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import us.codecraft.xsoup.Xsoup;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Bootstrap1 {
    private static Executor executor = Executors.newFixedThreadPool(1000);

    private static File f = new File("C:\\Users\\007_g\\Desktop\\dw-4.txt");

    private static AtomicInteger atomicInteger = new AtomicInteger(1);

    private static AtomicInteger sellout = new AtomicInteger(0);

    private static AtomicInteger up_95 = new AtomicInteger(0);

    //插件式配置生成HttpClient时所需参数（超时、连接池、ssl、重试）
    private static HCB hcb          //重试5次
            ;

    static {
        try {
            hcb = (HCB) HCB.custom()
                    .timeout(5000) //超时
                    .pool(2000, 1000)
                    .sslpv(SSLs.SSLProtocolVersion.TLSv1_2)    //设置ssl版本号，默认SSLv3，也可以调用sslpv("TLSv1.2")
                    .ssl() //https，支持自定义ssl证书路径和密码，ssl(String keyStorePath, String keyStorepass)
                    .retry(5);


        } catch (HttpProcessException e) {

        }
    }
        private static HttpClient client = hcb.build();

    private static class Worker implements Runnable {


        public void run() {
                work();

        }

        private void work() {
            {
                int retry = 3;
                int nowRetry = 0;
                for (int detailId = 0; detailId < 100000; ) {
                        detailId = nowRetry > 0  && nowRetry < 3? detailId : atomicInteger.getAndAdd(1);
                        String detailIdStr = String.format("%05d", detailId);
                        String url = "https://www.duanrong.com/zqzr/detail/57" + detailIdStr;
//                    String url = "https://www.duanrong.com/zqzr/detail/5914913";
                        //---------------------------------
                        //			【详细说明】
                        //--------------------------------
                        //插件式配置Header（各种header信息、自定义header）
                    try {
                        Header[] headers = HttpHeader.custom()
                                .userAgent("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0;")
                                .referer("https://www.duanrong.com/zqzr/list")
                                .build();


                        Map<String, Object> map = new HashMap<String, Object>();
//                    map.put("key1", "value1");
//                    map.put("key2", "value2");

                        //插件式配置请求参数（网址、请求参数、编码、client）
                        HttpConfig config = HttpConfig.custom()
                                .client(client)
                                .headers(headers)    //设置headers，不需要时则无需设置
                                .url(url)    //设置请求的url
                                .map(map)    //设置请求参数，没有则无需设置
                                .encoding("utf-8");//设置请求和返回编码，默认就是Charset.defaultCharset()
                        //.client(client)	//如果只是简单使用，无需设置，会自动获取默认的一个client对象
                        //.inenc("utf-8") //设置请求编码，如果请求返回一直，不需要再单独设置
                        //.inenc("utf-8")	//设置返回编码，如果请求返回一直，不需要再单独设置
                        //.json("json字符串")     //json方式请求的话，就不用设置map方法，当然二者可以共用。
                        //.context(HttpCookies.custom().getContext()) //设置cookie，用于完成携带cookie的操作
                        //.out(new FileOutputStream("保存地址"))		//下载的话，设置这个方法,否则不要设置
                        //.files(new String[]{"d:/1.txt","d:/2.txt"})	//上传的话，传递文件路径，一般还需map配置，设置服务器保存路径

                        String html = HttpClientUtil.send(config);
                        nowRetry = 0;
                        Document document = Jsoup.parse(html);
                        if (!html.contains("确认购买")) {
                            sellout.addAndGet(1);
                            continue;
                        }
                        System.out.println(detailId);
                        String charge = Xsoup.compile("//li[@class=dif-width]/font/text()").evaluate(document).get().trim();
                        String leavingPeriod = Xsoup.compile("//ul[@class=part1-left-list]/li[2]/h3/text()").evaluate(document).get().trim();
                        String amount = Xsoup.compile("//ul[@class=part1-left-list]/li[3]/h3/text()").evaluate(document).get().trim();
                        String period = leavingPeriod.substring(0, 1);
                        int periodI = 100;
                        try {
                            periodI = Integer.parseInt(period);
                        } catch (Throwable t) {
                            periodI = 100;
                        }
                        int changeI = Integer.parseInt(charge.substring(0, charge.length() - 1));
                        if (changeI < 95) { // 85折 90天内
                            System.out.println("url:" + url + " get!");
                            StringBuilder sb = new StringBuilder();
                            sb.append(" "+ url).append(" " + charge).append(" " +amount).append(" " + leavingPeriod);
                            sb.append("\r\n");
                            FileUtils.write(f, sb.toString(), "UTF-8", true);
                        } else {
                            up_95.addAndGet(1);
                            System.out.println("url:" + url + "当前折扣为:" + charge);
                        }
                    } catch (Throwable t) {
                        nowRetry++;
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws HttpProcessException {
        System.setProperty("http.maxConnections", "1000");
        for (int i = 0; i < 50; i++) {
            executor.execute(new Worker());

        }
    }
}
