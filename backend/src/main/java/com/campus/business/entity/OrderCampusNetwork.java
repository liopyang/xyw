package com.campus.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("order_campus_network")
public class OrderCampusNetwork {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long orderId;
  private String studentNo;
  private String idCardLastSix;
  private String exportStatus;
}
