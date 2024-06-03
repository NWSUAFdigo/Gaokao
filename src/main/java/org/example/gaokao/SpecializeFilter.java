package org.example.gaokao;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hello world!
 */
public class SpecializeFilter {
    public static void main(String[] args) {
        /*File file = FileUtil.file(Consts.FILE_DIR + "school.txt");*/
        File file = FileUtil.file(Consts.FILE_DIR + "schools.txt");
        FileReader fileReader = FileReader.create(file);
        String content = fileReader.readString();
        List<Item> list = JSONUtil.toList(content, Item.class);
        /*FileWriter outputFile = FileWriter.create(FileUtil.file(Consts.FILE_DIR + "special_output.txt"), CharsetUtil.CHARSET_UTF_8);*/
        FileWriter outputFile = FileWriter.create(FileUtil.file(Consts.FILE_DIR + "specialize_filter.txt"), CharsetUtil.CHARSET_UTF_8);

        /*List<String> existSchools = FileReader.create(FileUtil.file(Consts.FILE_DIR + "exist_schools.txt")).readLines();
        list = list.stream().filter(item -> !existSchools.contains(item.name)).toList();*/

        AtomicInteger schoolCount = new AtomicInteger(1);
        int schoolTotal = list.size();
        list.forEach(item -> {
            Arrays.asList(2023, 2022, 2021, 2020, 2019).forEach(year -> {
                int page = 1;
                while (true) {
                    try {
                        Thread.sleep(RandomUtil.randomInt(3, 10) * 1000L);
                    } catch (Exception ignored) {

                    }

                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("local_batch_id", 7);
                    queryMap.put("local_province_id", 41);
                    queryMap.put("local_type_id", 1);
                    queryMap.put("page", page);
                    queryMap.put("school_id", item.schoolId);
                    queryMap.put("size", 10);
                    queryMap.put("uri", "apidata/api/gk/score/special");
                    queryMap.put("year", year);
                    queryMap.put("signsafe", "1ec8fb045f8b9896c20de9f1e904b91c");
                    UrlBuilder urlBuilder = UrlBuilder.of("https://api.zjzw.cn/web/api/").setQuery(UrlQuery.of(queryMap));
                    HttpResponse resp = HttpRequest.post(urlBuilder.build()).execute();
                    if (!resp.isOk()) {
                        continue;
                    }

                    JSONArray datas = JSONUtil.parseObj(resp.body()).getJSONObject("data").getJSONArray("item");
                    if (datas.isEmpty()) {
                        break;
                    }
                    ++page;
                    datas.forEach(data -> {
                        List<String> outList = new ArrayList<>();
                        outList.add(item.name);
                        outList.add(((JSONObject) data).getStr("spname"));
                        outList.add(((JSONObject) data).getStr("year"));
                        outList.add(((JSONObject) data).getStr("average"));
                        outList.add(((JSONObject) data).getStr("min"));
                        outList.add(((JSONObject) data).getStr("max"));
                        outList.add(((JSONObject) data).getStr("min_section"));
                        System.out.println("Current: " + schoolCount.get() + " Total: " + schoolTotal + " "
                                + String.join(",", outList));
                        outputFile.write(String.join(",", outList) + "\n", true);
                    });
                }
            });
            schoolCount.incrementAndGet();
        });




        /*list.forEach(item -> {
            item.scoreRankMap.forEach((key, val) -> {
                List<String> scoreRankList = new ArrayList<>();
                Arrays.asList(2023, 2022, 2021, 2020).forEach(year -> {
                    Optional<ScoreRank> optional = val.stream().filter(scoreRank -> scoreRank.year.equals(String.valueOf(year))).findFirst();
                    if (optional.isPresent()) {
                        scoreRankList.add(optional.get().year);
                        scoreRankList.add(optional.get().score);
                        scoreRankList.add(optional.get().rank);
                    } else {
                        scoreRankList.add(String.valueOf(year));
                        scoreRankList.add("");
                        scoreRankList.add("");
                    }
                });
                List<String> strList = new ArrayList<>();
                strList.add(item.name);
                strList.add(item.dualClassName);
                strList.add(key);
                strList.add(String.join(",", scoreRankList));
                System.out.println(String.join(",", strList));
            });

        });
        System.out.println(content);*/
    }

    @Data
    public static class Item {
        public String dualClassName;
        public String name;
        public int schoolId;
        public String provinceName;

        public Map<String, List<ScoreRank>> scoreRankMap = new HashMap<>();
    }

    @Data
    @AllArgsConstructor
    public static class ScoreRange {
        public int max;
        public int min;
    }

    @Data
    @AllArgsConstructor
    public static class ScoreRank {
        public String score;
        public String rank;
        public String year;
    }

    @Data
    @AllArgsConstructor
    public static class SpecialtyRank {
        public String name;
        public int year;
        public int avgScore;
        public int minScore;
        public int maxScore;
        public int minRank;
    }
}
