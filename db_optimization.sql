-- ==========================================================
-- 苍穹外卖项目 SQL 性能优化脚本 (慢查询与索引优化)
-- ==========================================================

-- 1. 优化 OrderMapper.xml 中的 pageQuery 和 getOrdersStatistics 涉及的查询
-- 痛点：频繁通过 status 和 order_time 进行过滤和排序，导致全表扫描和文件排序 (Using filesort)
-- 解决：建立复合索引 (status, order_time)，遵循最左前缀法则
CREATE INDEX idx_status_ordertime ON orders(status, order_time);

-- 2. 优化通过订单号 (number) 或手机号 (phone) 的精确或前缀匹配
-- 注意：如果业务允许只通过手机号前缀、订单号前缀查询，建议将 XML 中的 
-- `like concat('%', #{phone}, '%')` 改为 `like concat(#{phone}, '%')`，以利用索引。
CREATE INDEX idx_phone ON orders(phone);
CREATE UNIQUE INDEX idx_number ON orders(number);

-- 修改建议示例 (手动在 OrderMapper.xml 中修改):
-- <if test="phone != null and phone != ''">
--     and phone like concat(#{phone}, '%')
-- </if>
-- <if test="number != null and number != ''">
--     and number like concat(#{number}, '%')
-- </if>
