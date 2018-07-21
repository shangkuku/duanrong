import com.arronlong.httpclientutil.HttpClientUtil;
import com.arronlong.httpclientutil.builder.HCB;
import com.arronlong.httpclientutil.common.HttpConfig;
import com.arronlong.httpclientutil.common.HttpHeader;
import com.arronlong.httpclientutil.common.SSLs;
import com.arronlong.httpclientutil.exception.HttpProcessException;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.assertj.core.api.Assert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import us.codecraft.xsoup.Xsoup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bootstrap {
    private static Executor executor = Executors.newFixedThreadPool(20);

    private static class Worker implements Runnable {
        private int start;

        public Worker(int start) {
            this.start = start;
        }

        public void run() {
            try {
                work();
            } catch (HttpProcessException e) {
                e.printStackTrace();
            }
        }

        private void work() throws HttpProcessException {
            {
                for (int i = this.start; i < this.start + 20; i++) {
                    int pageNo = i;

                    String url = "https://www.duanrong.com/zqzr/list?pageNo=" + pageNo;
                    //---------------------------------
                    //			【详细说明】
                    //--------------------------------

                    //插件式配置Header（各种header信息、自定义header）
                    Header[] headers = HttpHeader.custom()
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36")
                            .referer("https://www.duanrong.com/zqzr/list?termMin=1&termMax=2&pageNo=" + (pageNo - 1))
                            .build();

                    //插件式配置生成HttpClient时所需参数（超时、连接池、ssl、重试）
                    HCB hcb = HCB.custom()
                            .timeout(1000) //超时
                            .pool(100, 10) //启用连接池，每个路由最大创建10个链接，总连接数限制为100个
                            .sslpv(SSLs.SSLProtocolVersion.TLSv1_2)    //设置ssl版本号，默认SSLv3，也可以调用sslpv("TLSv1.2")
                            .ssl()        //https，支持自定义ssl证书路径和密码，ssl(String keyStorePath, String keyStorepass)
                            .retry(5)        //重试5次
                            ;


                    HttpClient client = hcb.build();

                    Map<String, Object> map = new HashMap<String, Object>();
//                    map.put("key1", "value1");
//                    map.put("key2", "value2");

                    //插件式配置请求参数（网址、请求参数、编码、client）
                    HttpConfig config = HttpConfig.custom()
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

                    //最简单的使用：
                    String html = HttpClientUtil.get(config);
                    Document document = Jsoup.parse(html);

                    List<String> list = Xsoup.compile("//div[@class=jxxm-con]/ul/li").evaluate(document).list();
                    if (list == null || list.size() == 0) {
                        break;
//                        i = this.start;
//                        continue;
                    }
                    for (String s : list) {
                        Document node = Jsoup.parse(s);
                        String link = Xsoup.compile("//a/@href").evaluate(node).get();
                        List<String> stringList = Xsoup.compile("//span/text()").evaluate(node).list();
                        String name = stringList.get(0);
                        double chargeD;
                        try {
                            String charge = stringList.get(5).trim();
                            charge = charge.substring(0, charge.length() - 1);
                            chargeD = Double.parseDouble(charge);
                        } catch (Throwable t) {
                            chargeD = 100;
                        }
                        if (!name.contains("季")) {
                            System.out.print("链接:" + link);
                            System.out.print(",折扣:" + chargeD);
                            System.out.print(",剩余天数:" + stringList.get(6));
                            System.out.print(",剩余期数:" + stringList.get(7));
                            System.out.println();
                        }
                    }
//        List<String> list = Xsoup.compile("//tr/td/text()").evaluate(document).list();
                }
            }
        }
    }

    public static void main(String[] args) throws HttpProcessException {

//        executor.execute(new Worker(1));
        for (int i = 1; i < 200; i = i + 20) {
            executor.execute(new Worker(i));
        }
    }
}
