[toc]
# 使用Pandas的一点经验和技巧
<div id="update-time">2015-05-18 08:41:24</div>
<div id="create-time">2015-05-18 08:39:42</div>
<div id="blog-id">59</div>
<div id="tags"></div>

上周刚做完ijcai中阿里赞助的一个比赛的第一赛季，无缘前三，甚是可惜。整个过程中都是采用pandas来抽取特征的，总的来说相当畅快，因此希望能够分享些用pandas过程中的经验技巧（坑），其中包含了许多从stackoverflow上查找的答案（我尽量引出出处，好多我找不到原始问答了），但愿大家少走弯路。暂时先写了这7点，以后想到了再补充，另外我把github上的一个[pandas-cookbook](https://github.com/ia-cas/pandas-cookbook)翻译了一下，快速上手应该是够了，更多的还是遇到问题去查看官方文档。

##1.读取csv文件的时候，注意指定数据类型

`read_csv`函数默认会把数值转换成int或者float，但是有时候对于一些ID列而言，这并不是好事。一不小心对ID列加减乘除操作后根本不会有任何提示！此外，string型的数据和int型的数据在打印出来后是没有区别的，有时候你明明看到一个df里有index为12345的这一行数据，df.ix['12345']返回的却是空，很可能原始的index是int型的，换成df.ix[12345]就行了。

##2.灵活使用map/applymap函数

applymap是针对DataFrame类型的，而map则是针对Series类型的。而且，根据文档里的描述map可接收的变量类型要比applymap更丰富，除了函数之外还有dict和Series类型。因此我通常用map来完成一些String类型变量到int类型变量的转换操作，以及一些变量的拉伸（log）剪裁异常值处理等。

##3.选取指定行和列

这个看起来很简单，不过实际操作起来有不少值得注意的地方，再加上官方文档写得很长很绕，很多时候都不知道该怎么用。

首先，说个好习惯。选取指定列的时候，一般有两种方法，一是直接用`df['col_1']`，还有就是`df.col_1`。在列名和默认函数不冲突的情况下，个人建议如果是新增一列，一般在等号`=`左边使用`df['col_1']`的形式，如果是更新一列数据，一般使用`df.col_1`的形式。在等号`=`右边尽量使用`df.col_1`的形式。除非列名是1,2,3,4这种没法通过df.1的形式访问。这样子做的优点是代码结构清晰，一看就明白这是在添加还是更新操作。

关于按行切片，说实话很少用。因为大多时候不会直接选取某几行，更多的时候是做一个mask操作，通过条件表达式得到bool变量后再选取。

pandas的各种选取操作很容易让人搞混，[这里](http://stackoverflow.com/questions/28757389/loc-vs-iloc-vs-ix-vs-at-vs-iat)有人问了，总结起来就是：

- loc: only work on index
- iloc: work on position
- ix: You can get data from dataframe without to be this in the index
- at: get scalar values. It's a very fast loc
- iat: Get scalar values. I'ts a very fast iloc

后面两个先不管，首先来看，我们选取某一列数据的时候，可以通过df['a']来得到一个Series对象，但如果'a'不是列名的话，就会报'KeyError'的错。如果是要获取某一行呢？那就用df.loc['a']，这里’a'就是行的index，所以如果你已知某一行的index，就用.loc来访问这行数据。此外，如果只是知道在第几行，不知道确切的index，那么就用.iloc。切记，这里的iloc是integer-loc的缩写，我一开始理解为了index-loc的缩写，结果就乱了。。。最后的ix既可以接收index有可以接收行号作为参数，不过我一般尽量避免用这个。

来个进阶点的问题：

> 现在我有一个样本集df，我想将其随机打乱后抽取其中的N个作为训练集剩下的作为测试集，该怎么做呢？

先想想，答案见[这里](http://stackoverflow.com/questions/15923826/random-row-selection-in-pandas-dataframe)

##4.[怎么重命名某些列](http://stackoverflow.com/questions/11346283/renaming-columns-in-pandas)


`df.rename(columns={'$a': 'a', '$b': 'b'}, inplace=True)`

很多时候我们抽取特征时，需要对特征重命名之后再与其他特征merge以避免命名冲突。实际使用的时候，比上面的稍微会复杂点。例如

`df.rename(columns={x:x+'_user' for x in df.columns if not x.endswith('_user')},inplace=True)`

##5.如何合并多列数据得到一个新的DataFrame

我们知道pd.concat可以将多行数据合在一起，通过指定参数axis=1即可按列合并

##6.要习惯级联

有时候完成某个功能可能会需要很长的一句话才搞定，比如`df1.merge(df2,how='left').merge(df3,how='left').merge(df4')`等等。这样子写出来的代码比较丑，呃不过受限于python的格式要求，没法像java一样换行那样写得很优雅。但我自己仍觉得这样能避免过多的中间变量，有助于思维的连贯性～

##7.groupby的并行化

groupby相当好用，但是数据量特别大的时候，有个很头疼的问题，慢！

当然，增加参数sort=False可以稍微缓解这个问题，但是，还是不能忍。最关键的问题在于，单线程！服务器上有40个核，却只用了1个，太浪费了！所以简单写个多核的版本。（IPython Notebook据说可以很方便地写并行程序，但是，尝试后失败了@_@）

```python
from multiprocessing import Pool

def get_fs(group):
     # extract features
     # ......
 
     return pd.DataFrame({'f1':f1,'f2':f2},index=[0])

def get_parallel_features(groups):
    p = Pool()
    result = None
    try:
        result = pd.concat(p.map(get_fs, groups))
     except Exception as e:
         print "ERROR:",e.strerror
     finally:
         p.close()
         p.join()
     return result

features = get_parallel_features([gp for key, gp in df.groupby(['xxx'])])
```

不过上面代码有点问题，可以看到传入的数据是一个列表`[gp for key, gp in df.groupby(['xxx'])]`，如果数据量很大的化这样子一次性读进来是相当吃内存的。不过根据我在stackoverflow上查到的资料显示，即使传一个iterator过去也无济于事，因为Pool().map操作的内部还是会先把iterator的内容全部读出来然后再操作。所以这个并行版本有待进一步优化。


