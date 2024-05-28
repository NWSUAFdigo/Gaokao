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
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SchoolFilter {
    public static void main(String[] args) {

        /*FileReader fileReader = FileReader.create(FileUtil.file(Consts.FILE_DIR + "test.txt"));
        List<Item> curItems = JSONUtil.toList(fileReader.readString(), Item.class);
        List<String> curSchoolNames = curItems.stream().map(Item::getName).distinct().toList();*/

        final List<Item> finalDatas = new ArrayList<>();
        Map<Integer, ScoreRange> scoreMap = new HashMap<>();
        // 排名：1000-60000 查询夸克高考获取
        scoreMap.put(2023, new ScoreRange(671, 558));
        scoreMap.put(2022, new ScoreRange(658, 549));
        scoreMap.put(2021, new ScoreRange(674, 562));
        scoreMap.put(2020, new ScoreRange(685, 584));
        scoreMap.put(2019, new ScoreRange(664, 537));

        FileWriter fileWriter = FileWriter.create(FileUtil.file(Consts.FILE_DIR + "school_filter.txt"));
        scoreMap.forEach((key, val) -> {
            int page = 1;
            do {
                try {
                    Thread.sleep(RandomUtil.randomInt(3, 5) * 1000L); // 随机时间间隔
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                Map<String, Object> queryMap = new HashMap<>();
                queryMap.put("big_min", val.max);
                queryMap.put("small_min", val.min);
                queryMap.put("local_province_id", 41);
                queryMap.put("local_type_id", 1);
                queryMap.put("page", page);
                queryMap.put("size", 20);
                queryMap.put("uri", "apidata/api/gk/score/province");
                queryMap.put("year", key);
                queryMap.put("zslx", 0);
                queryMap.put("signsafe", "02a1495c02eee9694feb12375c2e7046");

                UrlBuilder urlBuilder = UrlBuilder.ofHttp("https://api.zjzw.cn/web/api/", CharsetUtil.CHARSET_UTF_8).setQuery(UrlQuery.of(queryMap));
                HttpResponse response = HttpRequest.of(urlBuilder).setMethod(Method.POST).execute();

                if (!response.isOk()) {
                    continue;
                }
                JSONArray jsonData = JSONUtil.parseObj(response.body()).getJSONObject("data").getJSONArray("item");
                if (jsonData.isEmpty()) {
                    break;
                }
                System.out.println("Current Year: " + key + " Page: " + page + " Size: " + jsonData.size());
                ++page;
                List<Item> data = JSONUtil.toList(jsonData, Item.class);
                finalDatas.addAll(data);
            } while (true);
        });

        List<Item> datas = finalDatas.stream().distinct().sorted(Comparator.comparing(Item::getSchoolId)).toList();
        /*datas = datas.stream().filter(item -> !curSchoolNames.contains(item.name)).toList();*/
        // 获取最低分数线和最低录取名次
        AtomicInteger schoolCount = new AtomicInteger(1);
        datas.forEach(data -> {
            scoreMap.keySet().forEach(year -> {
                try {
                    Thread.sleep(RandomUtil.randomInt(3, 10) * 1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("Current school: " + data.name + " Year: " + year + " Count: " + schoolCount.get());
                Map<String, Object> queryMap = new HashMap<>();
                queryMap.put("e_sort", "zslx_rank,min");
                queryMap.put("e_sorttype", "desc,desc");
                queryMap.put("local_province_id", 41);
                queryMap.put("local_type_id", 1);
                queryMap.put("school_id", data.schoolId);
                queryMap.put("page", 1);
                queryMap.put("size", 10);
                queryMap.put("uri", "apidata/api/gk/score/province");
                queryMap.put("year", year);
                queryMap.put("signsafe", "02a1495c02eee9694feb12375c2e7046");

                UrlBuilder urlBuilder = UrlBuilder.ofHttp("https://api.zjzw.cn/web/api/", CharsetUtil.CHARSET_UTF_8).setQuery(UrlQuery.of(queryMap));
                HttpResponse response = HttpRequest.of(urlBuilder).setMethod(Method.POST).execute();
                if (!response.isOk()) {
                    return;
                }
                JSONArray jsonArr = JSONUtil.parseObj(response.body()).getJSONObject("data").getJSONArray("item");
                if (jsonArr.isEmpty()) {
                    return;
                }
                jsonArr.forEach(jsonData -> {
                    String batchId = ((JSONObject) jsonData).getStr("local_batch_id");
                    if (batchId.equals("7")) {
                        String score = ((JSONObject) jsonData).getStr("min");
                        String rank = ((JSONObject) jsonData).getStr("min_section");
                        String hisYear = ((JSONObject) jsonData).getStr("year");
                        String type = ((JSONObject) jsonData).getStr("zslx_name");

                        data.scoreRankMap.computeIfAbsent(type, k -> new ArrayList<>());
                        data.scoreRankMap.get(type).add(new ScoreRank(score, rank, hisYear));
                    }
                });
            });

            // 写数据到文件
            data.scoreRankMap.forEach((type, val) -> {
                List<String> yearData = new ArrayList<>();
                yearData.add(data.name);
                yearData.add(data.dualClassName);
                yearData.add(type);
                yearData.add("");
                yearData.add("");
                yearData.add("");
                yearData.add("");
                scoreMap.keySet().forEach(year -> {
                    Optional<ScoreRank> optional = val.stream().filter(scoreRank -> scoreRank.year.equals(String.valueOf(year))).findFirst();
                    if (optional.isEmpty()) {
                        yearData.add("");
                        yearData.add("");
                        yearData.add("");
                    } else {
                        yearData.add(optional.get().year);
                        yearData.add(optional.get().score);
                        yearData.add(optional.get().rank);
                    }
                });
                fileWriter.write(String.join(",", yearData) + "\n", true);
            });

            schoolCount.incrementAndGet();
        });

        System.out.println(JSONUtil.toJsonStr(datas));
        fileWriter.write("==========\n", true);
        fileWriter.write("==========\n", true);
        fileWriter.write("==========\n", true);
        fileWriter.write(JSONUtil.toJsonStr(datas) + "\n", true);
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
}
