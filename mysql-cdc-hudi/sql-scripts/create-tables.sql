create table trade_info
( id BIGINT NOT NULL,
  stock_symbol CHAR(5) NOT NULL,
  shares DECIMAL(18,4) NOT NULL,
  share_price DECIMAL(18,4) NOT NULL,
  trade_time DATETIME(6) NOT NULL,
  trader_id BIGINT NOT NULL,
  status VARCHAR(10) NOT NUll
  constraint trade_info_pk primary key(id)
);