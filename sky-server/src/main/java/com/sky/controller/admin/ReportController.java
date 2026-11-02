package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Api(tags = "数据统计相关接口")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 指定时间内的营业额统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("统计{}到{}的营业额", begin,end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }

    /**
     * 指定时间内的用户统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("统计{}到{}的用户数据", begin,end);
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    /**
     * 指定时间内订单统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("统计{}到{}的订单数据", begin,end);
        OrderReportVO vo = reportService.getOrdersStatistics(begin, end);
        log.info("返回数据: totalOrderCount={}, validOrderCount={}, orderCompletionRate={}", 
                vo.getTotalOrderCount(), vo.getValidOrderCount(), vo.getOrderCompletionRate());
        return Result.success(vo);
    }

    /**
     * 查询销量排名top10
     * @return
     */
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("查询{}到{}销量排名top10", begin,end);
        SalesTop10ReportVO vo = reportService.getSalesTop10(begin, end);
        return Result.success(vo);
    }

    @GetMapping("/export")
    @ApiOperation("导出数据统计报表")
    public void export(HttpServletResponse response) throws IOException {
        log.info("导出数据统计报表");
        reportService.export(response);
    }
}
