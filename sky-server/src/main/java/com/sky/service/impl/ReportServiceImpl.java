package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 指定时间内的营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = getDateList(begin, end);

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询每天的营业额数据
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        String join = StringUtils.join(dateList, ",");
        String join1 = StringUtils.join(turnoverList, ",");

        TurnoverReportVO build = TurnoverReportVO.builder()
                .dateList(join)
                .turnoverList(join1)
                .build();

        return build;
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = getDateList(begin, end);

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        // 记录前一天的总用户数
        Integer previousTotalUser = 0;

        for (LocalDate date : dateList) {
            // 查询截止到当天的总用户数
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("end", endTime);

            Integer currentTotalUser = userMapper.countByMap(map);

            // 当天新增用户数 = 当前总用户数 - 前一天总用户数
            Integer newUser = currentTotalUser - previousTotalUser;

            totalUserList.add(currentTotalUser == null ? 0 : currentTotalUser);
            newUserList.add(newUser == null ? 0 : newUser);

            // 更新前一天的总用户数
            previousTotalUser = currentTotalUser == null ? 0 : currentTotalUser;
        }

        String join = StringUtils.join(dateList, ",");
        String join1 = StringUtils.join(totalUserList, ",");
        String join2 = StringUtils.join(newUserList, ",");

        UserReportVO build = UserReportVO.builder()
                .dateList(join)
                .totalUserList(join1)
                .newUserList(join2)
                .build();

        return build;
    }

    /**
     * 获取指定时间范围内的日期列表
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = getDateList(begin, end);

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        // 累计总订单数和有效订单数
        int totalOrderCount = 0;
        int validOrderCount = 0;

        for (LocalDate date : dateList) {

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            Map<String, Object> orderCount = orderMapper.getOrdersStatistics(map);
            
            // 安全的类型转换处理
            Object total = orderCount.get("totalOrders");
            Object valid = orderCount.get("validOrders");

            int totalOrders = total == null ? 0 : ((Number) total).intValue();
            int validOrders = valid == null ? 0 : ((Number) valid).intValue();

            // 累加到总数
            totalOrderCount += totalOrders;
            validOrderCount += validOrders;

            orderCountList.add(totalOrders);
            validOrderCountList.add(validOrders);
        }

        String join = StringUtils.join(dateList, ",");
        String join1 = StringUtils.join(orderCountList, ",");
        String join2 = StringUtils.join(validOrderCountList, ",");

        // 计算订单完成率（返回百分比格式，0-100之间）
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = (validOrderCount + 0.0) / totalOrderCount;
        }


        OrderReportVO build = OrderReportVO.builder()
                .dateList(join)
                .orderCountList(join1)
                .validOrderCountList(join2)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

        return build;
    }

    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        Map<String, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);

//        List<String> nameList = new ArrayList<>();
//        List<String> numberList = new ArrayList<>();

        List<GoodsSalesDTO> top10 = orderMapper.getSalesTop10(map);

//        for (GoodsSalesDTO item : top10) {
//            nameList.add(item.getName());
//            numberList.add(String.valueOf(item.getNumber()));
//        }
        List<String> nameList = top10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = top10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());


        String joinName = StringUtils.join(nameList, ",");
        String joinNumber = StringUtils.join(numberList, ",");

        SalesTop10ReportVO build = SalesTop10ReportVO.builder()
                .nameList(joinName)
                .numberList(joinNumber)
                .build();

        return build;
    }

    public void export(HttpServletResponse response) throws IOException {
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        LocalDateTime dateTimeBegin = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime dateTimeEnd = LocalDateTime.of(dateEnd, LocalTime.MAX);

        //查询概览数据
        BusinessDataVO businessData = workspaceService.getBusinessData(dateTimeBegin, dateTimeEnd);

        //通过POI将数据写入到Excel模板中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("templates/运营数据报表模板.xlsx");

        //基于模板文件创建Excel对象
        XSSFWorkbook excel = new XSSFWorkbook(in);

        //获取表格文件的sheet标签页
        XSSFSheet sheet = excel.getSheet("Sheet1");

        //填充数据--时间
        sheet.getRow(1).getCell(1).setCellValue("时间" + dateBegin + "至" + dateEnd);

        sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
        sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
        sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());

        sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
        sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

        //填充明细数据
        for (int i = 0; i < 30; i++) {
            LocalDate date = dateBegin.plusDays(i);
            //查询某一天的营业数据
            BusinessDataVO businessData1 = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

            XSSFRow row = sheet.getRow(7 + i);
            row.getCell(1).setCellValue(date.toString());
            row.getCell(2).setCellValue(businessData1.getTurnover());
            row.getCell(3).setCellValue(businessData1.getValidOrderCount());
            row.getCell(4).setCellValue(businessData1.getOrderCompletionRate());
            row.getCell(5).setCellValue(businessData1.getUnitPrice());
            row.getCell(6).setCellValue(businessData1.getNewUsers());
        }

        ServletOutputStream outputStream = response.getOutputStream();
        excel.write(outputStream);

        excel.close();
        outputStream.close();
        in.close();
    }

    private static List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }
}
