/**
 * 
 */
package com.aksh.rand.mysql;

import java.util.Date;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;


/**
 * @author rawaaksh
 *
 */
@Component
public interface TradeInfoMapper {

    @Select("SELECT * FROM trade_info WHERE id = #{id}")
    TradeInfo getTrade(@Param("id") Long id);

    @Insert("INSERT INTO trade_info "
    		+ "VALUES (#{id}, #{stock_symbol}, #{shares}, #{share_price}, #{trade_time},#{trader_id},#{status});")
    void insertTrade(@Param("id") Long id,
                     @Param("stock_symbol") String stock_symbol,
                     @Param("shares") Double shares,
                     @Param("share_price") Double share_price,
                     @Param("trade_time") Date trade_time,
                     @Param("trader_id") Long trader_id,
                     @Param("status") String status);

    @Update("UPDATE trade_info "
            + "SET shares= #{shares}, share_price= #{share_price}, " +
            "  trade_time= #{trade_time}, status= #{status}" +
            " WHERE id = #{id} ;")
    void updateTrade(@Param("id") Long id,
                     @Param("shares") Double shares,
                     @Param("share_price") Double share_price,
                     @Param("trade_time") Date trade_time,
                     @Param("status") String status);

}