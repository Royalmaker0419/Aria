# 矩形物品摆放算法 - Rectangle Placement

## 1. AI 使用说明

### 你使用了哪些 AI 工具
- **Trae IDE** (DeepSeek-V4-Pro) — 主要编码助手
- **Plan agent** — 算法设计和结构规划

### AI 主要帮助了哪些部分
- 思路拆解：将整个问题分解为多个模块（数据解析、多边形光栅化、放置算法），每个模块设计清晰的接口
- 代码生成：生成完整Java代码框架和实现细节
- 几何算法实现：射线扫描算法、点-in-多边形测试的实现细节
- Debug：帮助定位坐标转换中的错误

### 哪些关键逻辑是你自己理解并调整的
- 墙贴放优先级的评分函数设计：将墙面相邻权重设最高，距离墙面距离作为惩罚项
- 冰箱开门边识别：根据哪一侧靠墙确定开门方向，预留一侧空间
- 内开门 N×N 区域识别：通过中点测试确定法线方向，然后标记该区域
- 放置顺序策略：大物件（冰箱、制冰机）优先放置，小物件放最后填充空隙
- 网格粒度选择：cellSize=10.0，因为所有物品尺寸都刚好被10整除

---

## 核心代码实现逻辑说明

### 问题描述
给定房间轮廓、门位置、若干矩形物品，将物品不重叠地摆放在房间内。物品可旋转90度，空间足够时优先贴墙放置。不可遮挡门，内开门会占据N×N空间。冰箱长度一侧为开门边，该侧不能放其他东西。

### 算法框架

#### 1. 网格化方法
选择 10.0 单位作为网格单元大小。所有输入物品尺寸（400, 600, 760, 850, 1000, 1220, 1330）都刚好被10整除，避免了四舍五入误差。将房间坐标归一化到网格：
```
minX = 所有边界x最小值
minY = 所有边界y最小值
gridX = round((x - minX) / 10)
gridY = round((y - minY) / 10)
```
最大网格尺寸约为 800 × 200 ≈ 160,000 格，计算量完全可控。

#### 2. 多边形光栅化
使用**扫描线射线投射**算法：
- 对每一行网格，计算该行的中心点y坐标
- 收集所有多边形边与水平线的交点
- 按x排序交点，成对标记为内部
- 边界通过边遍历标记所有经过多边形边的单元格为BOUNDARY

多边形顶点顺序不需要预先知道方向，射线算法自动处理奇偶性。

#### 3. 内开门处理
- 计算门的两个端点距离得到宽度N
- 计算门的中点，得到两个垂直于门的法线方向
- 使用点-in-多边形测试，确定哪一个法线方向指向房间内部
- 将从门向内延伸的N×N矩形区域标记为BLOCKED，物品不可放在这里

#### 4. 贴墙距离计算
使用**多源BFS**：
- 从所有BOUNDARY单元格开始，距离为0
- BFS遍历所有FREE单元格，每个单元格记录距离最近墙面的步数
- 距离越大表示越远离墙壁

#### 5. 放置顺序
按以下优先级：
1. 冰箱（最大，有开门约束）→ 最先放
2. 制冰机（第二大）
3. 货架（面积比离地架大）
4. 离地架（最小，最灵活）→ 最后放

同一类型物品按面积从大到小，同名按字典序。

#### 6. 评分函数（贴墙优先）
对每个可能放置位置打分：
```
score = 0
每侧都检查：
  如果相邻单元格都是BOUNDARY → +100分
  如果相邻单元格都是OCCUPIED（其他已放物品）→ +30分
如果两侧都是墙（角落）→ +50分
惩罚 = 平均墙距 × 5 → score -= 惩罚
```
总分越高越好。这样自然会优先选择贴墙放置。

#### 7. 冰箱开门边处理
- 冰箱长度=1220为开门边，根据放置方向确定开门边在哪一侧
- 如果只有一侧靠墙 → 开门边在对侧（冰箱背靠墙，门朝房间）
- 如果两侧都靠墙或都不靠墙 → 选择自由空间更多的一侧
- 在开门侧标记1个单元格（10单位）为BLOCKED，不允许其他物品放置

### 复杂度分析
假设房间W×H个网格，总共有K件物品。对于每件物品，最坏情况要搜索 (W - w)*(H - h)*2 个位置，实际因为墙优先评分，能较早找到好位置。对于题目给出的例子，所有情况都可以在几百毫秒内完成。

---

## 运行环境及运行方式

### 环境要求
- Java 17+
- Maven 3.6+

### 编译打包
```bash
cd rectangle-placement
mvn clean package
```

生成可执行jar在 `target/rectangle-placement-1.0.0.jar`

### 运行命令
```bash
java -jar target/rectangle-placement-1.0.0.jar <input.json>
```

输出结果以JSON格式打印到stdout。

示例：
```bash
java -jar target/rectangle-placement-1.0.0.jar examples/example1.json
java -jar target/rectangle-placement-1.0.0.jar examples/example2.json
java -jar target/rectangle-placement-1.0.0.jar examples/example3.json
java -jar target/rectangle-placement-1.0.0.jar examples/example4.json
```

### 示例输出格式
```json
{
  "feasible" : true,
  "placements" : {
    "fridge" : {
      "cx" : 56520.0,
      "cy" : 36485.0,
      "rotation" : 0
    },
    "shelf-1" : {
      "cx" : 56410.0,
      "cy" : 29690.0,
      "rotation" : 0
    }
  }
}
```

如果不可行：
```json
{
  "feasible": false,
  "placements": {}
}
```

---

## 既定输入的输出示例

### example1.json 输出
```json
{
  "feasible" : true,
  "placements" : {
    "fridge" : {
      "cx" : 6510.0,
      "cy" : 29415.0,
      "rotation" : 0
    },
    "iceMaker" : {
      "cx" : 6740.0,
      "cy" : 32525.0,
      "rotation" : 0
    },
    "shelf-1" : {
      "cx" : 6920.0,
      "cy" : 31600.0,
      "rotation" : 90
    },
    "shelf-2" : {
      "cx" : 6920.0,
      "cy" : 30600.0,
      "rotation" : 90
    },
    "shelf-3" : {
      "cx" : 4900.0,
      "cy" : 31980.0,
      "rotation" : 90
    },
    "overShelf-1" : {
      "cx" : 5600.0,
      "cy" : 29590.0,
      "rotation" : 0
    },
    "overShelf-2" : {
      "cx" : 6060.0,
      "cy" : 32580.0,
      "rotation" : 0
    },
    "overShelf-3" : {
      "cx" : 5360.0,
      "cy" : 30130.0,
      "rotation" : 90
    }
  }
}
```

### example2.json 输出
```json
{
  "feasible" : true,
  "placements" : {
    "fridge" : {
      "cx" : 29610.0,
      "cy" : 32675.0,
      "rotation" : 0
    },
    "shelf-1" : {
      "cx" : 29500.0,
      "cy" : 34770.0,
      "rotation" : 0
    },
    "shelf-2" : {
      "cx" : 31100.0,
      "cy" : 34770.0,
      "rotation" : 0
    },
    "shelf-3" : {
      "cx" : 31400.0,
      "cy" : 32550.0,
      "rotation" : 90
    },
    "shelf-4" : {
      "cx" : 29200.0,
      "cy" : 34070.0,
      "rotation" : 90
    },
    "overShelf-1" : {
      "cx" : 30300.0,
      "cy" : 34770.0,
      "rotation" : 0
    },
    "overShelf-2" : {
      "cx" : 30520.0,
      "cy" : 32210.0,
      "rotation" : 0
    },
    "overShelf-3" : {
      "cx" : 31400.0,
      "cy" : 34270.0,
      "rotation" : 90
    }
  }
}
```

### example3.json 输出（内开门）
```json
{
  "feasible" : true,
  "placements" : {
    "fridge" : {
      "cx" : 56520.0,
      "cy" : 36485.0,
      "rotation" : 0
    },
    "shelf-1" : {
      "cx" : 56410.0,
      "cy" : 29690.0,
      "rotation" : 0
    },
    "shelf-2" : {
      "cx" : 56810.0,
      "cy" : 37350.0,
      "rotation" : 0
    },
    "shelf-3" : {
      "cx" : 56110.0,
      "cy" : 30390.0,
      "rotation" : 90
    },
    "shelf-4" : {
      "cx" : 56110.0,
      "cy" : 31390.0,
      "rotation" : 90
    },
    "shelf-5" : {
      "cx" : 56110.0,
      "cy" : 32390.0,
      "rotation" : 90
    },
    "overShelf-1" : {
      "cx" : 57750.0,
      "cy" : 35660.0,
      "rotation" : 0
    },
    "overShelf-2" : {
      "cx" : 57750.0,
      "cy" : 37350.0,
      "rotation" : 0
    },
    "overShelf-3" : {
      "cx" : 56110.0,
      "cy" : 33190.0,
      "rotation" : 90
    }
  }
}
```

### example4.json 输出
```json
{
  "feasible" : true,
  "placements" : {
    "fridge" : {
      "cx" : 183545.0,
      "cy" : 32490.0,
      "rotation" : 90
    },
    "shelf-1" : {
      "cx" : 183380.0,
      "cy" : 29750.0,
      "rotation" : 0
    },
    "shelf-2" : {
      "cx" : 184570.0,
      "cy" : 32600.0,
      "rotation" : 90
    },
    "overShelf-1" : {
      "cx" : 184170.0,
      "cy" : 29850.0,
      "rotation" : 90
    },
    "overShelf-2" : {
      "cx" : 183080.0,
      "cy" : 30250.0,
      "rotation" : 90
    },
    "overShelf-3" : {
      "cx" : 183080.0,
      "cy" : 30850.0,
      "rotation" : 90
    }
  }
}
```

---

## 项目结构
```
rectangle-placement/
├── pom.xml
├── README.md
├── src/
│   └── main/
│       └── java/com/takehome/
│           ├── App.java                 # 入口，CLI读取输入
│           ├── model/
│           │   ├── InputData.java       # JSON输入解析
│           │   ├── Placement.java       # 单个物品摆放结果
│           │   └── PlacementResult.java # 整体结果
│           ├── core/
│           │   ├── Grid.java            # 网格数据结构
│           │   ├── GeometryUtils.java   # 几何工具
│           │   └── PolygonRasterizer.java # 多边形光栅化+墙距离+门处理
│           └── placer/
│               ├── GreedyPlacer.java    # 贪心放置主算法
│               └── PlacementScorer.java # 评分函数
└── examples/
    ├── example1.json
    ├── example2.json
    ├── example3.json
    └── example4.json
```

---

## 边界情况处理

1. **门被跳过**：多边形光栅化时会跳过门所在边，保持开门间隙
2. **浮点精度**：cellSize=10.0刚好容纳所有物品尺寸，浮点数不影响结果
3. **冰箱开门方向**：自动选择与墙相对的一侧作为开门边，符合常识
4. **内开门区域**：只标记房间内区域，不影响外面
5. **狭窄通道**：算法自动尝试两种旋转，自然选择能放进去的那个

