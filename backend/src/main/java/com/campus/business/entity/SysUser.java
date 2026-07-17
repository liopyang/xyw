package com.campus.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_user")
public class SysUser {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String username;
  private String phone;
  private String password;
  private String realName;
  private String roleCode;
  private Integer status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @TableLogic private Integer deleted;
}
