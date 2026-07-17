package com.campus.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("agent")
public class Agent {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String agentNo;
  private Long userId;
  private String name;
  private String phone;
  private String level;
  private Integer status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @TableLogic private Integer deleted;
}
