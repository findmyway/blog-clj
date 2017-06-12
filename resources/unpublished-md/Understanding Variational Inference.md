<script type="text/x-mathjax-config">
MathJax.Hub.Config({
  TeX: { equationNumbers: { autoNumber: "AMS" } }
});
</script>
[toc]

<div id="tags">TEST</div>

# 深入理解变分推断

## 熵（Entropy）

以下借用《Statistical Rethinking》一书中的部分内容来理解Entropy及其相关的内容。

假设今天天气预报告诉我们，明天有可能下雨（记为事件A），该事件有一定的不确定性，等到第二天结束的时候，不论第二天是否下了雨，之前的不确定性都消失了。换句话说，在第二天看到事件A的结果时（下雨或没下雨），我们获取了一定的信息。

> **信息**：在观测到某一事件发生的结果之后，不确定性的降低程度。

直观上，衡量信息的指标需要满足一下三点：

1. 连续性。如果该指标不满足连续性，那么一点微小的概率变化会导致很大的不确定性的变化。
2. 递增性。随着可能发生的事件越多，不确定性越大。比如有两个城市需要预测天气，A城市的有一半的可能下雨，一半的可能是晴天，而B城市下雨、下冰雹和晴天的概率分别为1/3，那么我们希望B城市的不确定性更大一些，毕竟可能性空间更大。
3. 叠加性。将明天是否下雨记为事件A，明天是否刮风记为事件B，假设二者相互独立，那么将事件A的不确定性与事件B的不确定性之和，与（下雨/刮风、不下雨/刮风、下雨/不刮风、不下雨/不刮风）这四个事件发生的不确定性之和相等。

信息熵的表达形式刚好满足以上三点：

$$
\begin{equation}
\begin{split}
H(p) & = - \mathbb{E} \log \left(p_i\right) \\
     & = - \sum_{i=1}^{n} p_i \log\left(p_i \right) 
\end{split}
\end{equation} $$

简单来说，熵就是概率对数的加权平均。

## K-L散度（Kullback-Leibler Divergence ）

> **散度**:用某个分布去描述另外一个分布时引入的不确定性。

散度的定义如下：

$$
\begin{equation}
\begin{split}
D_{KL}(p,q) & = \sum_{i \in I} p_i  \left( \log (p_i) - \log (q_i) \right) \\
       & = \sum_{i \in I} p_i \log \left( \frac {p_i} {q_i} \right) 
\end{split}
\label{KL}
\end{equation}
$$

KL散度是大于等于0的，可以通过[Gibb's不等式][Gibb's inequality]证明：

首先，我们知道：

$$
\begin{equation}
\ln x \le x - 1
\end{equation}
$$

于是，根据$\eqref{KL}$中KL散度的定义，可以得到如下不等式：

$$
\begin{equation}
\begin{split}
-\sum_{i \in I} p_i \log \left( \frac {q_i} {p_i} \right) & \ge - \sum_{i \in I} p_i \left( \frac {q_i - p_i} {p_i}\right) \\
 &= -\sum_{i \in I} (q_i - p_i) \\
 &= 1 - \sum_{i \in I} q_i \\
 &\ge 0
\end{split}
\end{equation}
$$

可以看出，只有当两个分布一一对应相等的时候才取0。

如果将KL散度拆开，可以看作是交叉熵与信息熵之差：

$$
$$

[Gibb's inequality]:https://en.wikipedia.org/wiki/Gibbs%27_inequality