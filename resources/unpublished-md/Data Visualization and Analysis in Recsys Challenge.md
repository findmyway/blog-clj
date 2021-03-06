[toc]
# Data Visualization and Analysis in Recsys Challenge
<div id="update-time">2015-02-03 07:23:41</div>
<div id="create-time">2015-01-21 00:51:00</div>
<div id="blog-id">56</div>
<div id="tags">DataVisualization</div>
本来想写点关于[pandas](http://pandas.pydata.org/)的，写了点后感觉，毕竟只是个工具，怎么写都只会变成技术文档。还是来点实际的In Acion之类的东西，所以本文就结合pandas和[Recsys Challenge 2015](http://2015.recsyschallenge.com/)的数据来做一些简单分析。侧重点在如何理解和分析数据。

- 数据的下载和描述见[这里](http://2015.recsyschallenge.com/challenge.html)

- 下文中绘图代码在[这里](http://nbviewer.ipython.org/gist/findmyway/8802f75eacc14c107993)，（不建议在低于16G内存的机器上直接跑该脚本，中间变量很吃内存。某些不需要的中间变量可以del后释放掉。）

- 原始网页的Leaderboard 太丑了...我做了个更Fancy的，有兴趣看看[这里](http://recsys.tianjun.ml)

另外本文参考了以下内容：

> 1. [http://aloneindecember.com/words/recsys-challenge-part-i/](http://aloneindecember.com/words/recsys-challenge-part-i/)

> 2. [http://aloneindecember.com/words/recsys-challenge-part-ii/](http://aloneindecember.com/words/recsys-challenge-part-ii/)

> 3. [Can we approximate the upper bound score for the 2015 recsys challenge? | Recommended](https://thierrysilbermann.wordpress.com/2015/01/13/can-we-approximate-the-upper-bound-score-for-the-2015-recsys-challenge/)

> 4. [土人之NLP日志: How to build a naive (very naive) system scored over 30,000 in RecSysChallenge 2015?](http://playwithnlp.blogspot.sg/2014/12/how-to-build-naive-very-naive-system.html)


##1. 加载数据

首先加载数据，pandas(以下简称pd)的[read_csv](http://pandas.pydata.org/pandas-docs/stable/generated/pandas.io.parsers.read_csv.html)函数相当丰富，但基本参数和np的loadtxt比较接近，主要差别在于，pd对于二维数据采用了类似SQL的处理方式，读入数据的时候会涉及到Index列的处理。另外结合数据描述，在读入数据的时候需要将TimeStamp设置为datetime.datetime类型。

##2. 准备基础数据

pd可以对数据做一些类似SQL的处理，本文最常用的一个函数便是groupby。由于click和buy数据分别由两个文件存放，为了便于分析，首先将click和buy中相同的字段通过concat连接在一起得到total。提取buy中所有sessID，然后在total中增加一列IsBuy来区分每一条record所属的sessID最后是否有购买行为。分别对click，buy，total按照SessionID分组，得到click_sesID_group，buy_sesID_group和total_sesID_group。针对click中的category信息，构建items2category的字典。


##3. 数据规模初探

对分组后的数据简单的总数统计可以发现，最后购买了东西的session的平均点击次数是8.74次，而没买东西的session所对应的平均点击次数却只有3.2次。显然这合乎常理，**在确认购买东西之前，人们会反复查看和对比。如果只是随便点击两下似乎直接购买的可能不大。**

通常，为了更可视化地描述人们在每个session中的点击次数，会画出如下的直方图来。

![a.png_a9b6f1920752df9ac5ec07ab83a06369](http://ontheroad.qiniudn.com/blog/resources/a.png_a9b6f1920752df9ac5ec07ab83a06369/w660)

看到这张图的第一时间，你可能会说，“噢，典型的泊松分布”，然后，没有然后了。其实这样的表示带来的信息量很有限。前面提到点击和购买session的平均次数差异很大，为了可视化地表示出这种差异，不妨将两部分数据拆分开来，选取[0,25]这一密集的区间，将二者的数据做对比，用图来说话。

![b.png_bae06f391292f2407b24beface0acaaa](http://ontheroad.qiniudn.com/blog/resources/b.png_bae06f391292f2407b24beface0acaaa/w660)

这里做了一个归一化处理。这样看起来就清楚多了。对于click数据，大部分session都只点了两三次，而且随着点击次数的增加，session个数衰减得非常快。而buy数据（最后产生了购买行为的session）则显示，其衰减的速度要缓慢得多。如果一个session的点击次数超过了10，那么极大可能会产生购买行为。

##4. 加入时间序列分析

我们把session作为一个整体来看待，在这里选取每个session的第一次点击记录来代表。将total_sesID_group中的每个小组按照时间排序后，用first()函数得到每个小组的第一条记录。用apply函数将datetime类型的TimeStamp转换成月，然后绘制直方图，同时记录购买点击比例。

<pre xml:space="preserve">
</pre>
![c.png_91ca2e0846a89cd147e3533244ac5e30](http://ontheroad.qiniudn.com/blog/resources/c.png_91ca2e0846a89cd147e3533244ac5e30/w660)

虽然点击量和购买量在7月8月有一定的起伏，但是比例整体保持不变。再将TimeStamp转换成星期，分析更细致的变化趋势。

![d.png_b8c026947f2c28889731b0ede52192e5](http://ontheroad.qiniudn.com/blog/resources/d.png_b8c026947f2c28889731b0ede52192e5/w660)


从每周的成交量来看，大致还算比较稳定，在均值0.055附近抖动，而且第26周附近，点击量和购买量虽然同时跳水，但比例几乎保持不变。需要注意的是，由于测试集是从与训练集相同时间段中抽出来的一部分session，这样的稳定性有利于数据的一致性分析。

继续细化，将时间细化成天，得到下图。

![e.png_3b1b190ad55cb7818b3c5d8a35703090](http://ontheroad.qiniudn.com/blog/resources/e.png_3b1b190ad55cb7818b3c5d8a35703090/w660)

规律性相当强，几乎每到周二就跌到谷底，在周日达到峰值。有点意思。那再具体看看一个星期里，每天的点击量购买量究竟差别多大。

![f.png_5948456e64dfab1166e9b9fdc6c17525](http://ontheroad.qiniudn.com/blog/resources/f.png_5948456e64dfab1166e9b9fdc6c17525/w660)

周二的点击量降到了高峰时期的1/3，购买点击比降到了一半。继续细化。绘制24小时流量图如下。

![g.png_a6b1bbdc7972a1b185e137ad0a1321ae](http://ontheroad.qiniudn.com/blog/resources/g.png_a6b1bbdc7972a1b185e137ad0a1321ae/w660)

9~10点附近一个高峰，18~19点附近一个高峰。

继续细化。（细化无止境...)

考虑这样一个问题，最后产生购买的session和没有购买的session之间在点击时间上会有怎样的差别呢？会不会最后产生购买的用户在一个页面上倾向于花更多的时间？带着这个问题，分别将点击session和购买session相邻两次点击之间的时间差求平均后表示出来。（横轴表示第x个点击，纵轴表示该次点击与距离上一次点击的平均时间。

![h.png_3057f6bac63838a0e44ba4bacc8ead9d](http://ontheroad.qiniudn.com/blog/resources/h.png_3057f6bac63838a0e44ba4bacc8ead9d/w660)

两条线的交汇点大概是（10，120）附近。一定程度上验证了前面的设想。但随着点击次数的增加，购买的曲线和点击的曲线差不多杂糅在一起。可能点击次数太大后，单从时间因素无法区分用户是否会购买。（这张图的绘制含义描述起来稍微有点绕，有兴趣可以去看源码如何画出来的，就更好理解了）个人比较疑惑的一点是，为什么头两次点击之间的时间会相差3到4分钟这么长......

##5. Category和Price信息

由于Category和price（quantity）信息都有很多缺失值，这一点需要格外注意。先说Category信息，用面积来表示每一类的每天的点击量，选取主要的几类绘制如下：

![i.png_c1847099bcd3e8a287e422412cb82ecf](http://ontheroad.qiniudn.com/blog/resources/i.png_c1847099bcd3e8a287e422412cb82ecf/w660)


可以看到，Category的缺失信息集中在上半年（category为0表示类别信息缺失），下半年打折类别的点击量很多（类别为S表示打折）。因此在后面分析的时候需要格外注意该分界线。

同样的方法分析Price信息。

![j.png_c869d85b15b2346775ce97c8b1d034b9](http://ontheroad.qiniudn.com/blog/resources/j.png_c869d85b15b2346775ce97c8b1d034b9/w660)

有意思的是缺失值占了一半，但却集中在中间那几个月。如果要利用price和quantity信息的化，恐怕需要作分区处理了。

最后看一下价格的波动区间吧。其实下图用kde更合适，而不是直方图。
![k.png_4d8865499475d03c8cfd97131711e7de](http://ontheroad.qiniudn.com/blog/resources/k.png_4d8865499475d03c8cfd97131711e7de/w660)
