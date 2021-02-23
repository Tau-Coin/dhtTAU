#### Get operation during put item in libtorrent

这里我们向提高put操作的效率，分析知道put item之前会执行get nodes的操作；

这个get过程起两个作用：

 - 拿到距离target相近的节点

 - 获取这些节点的write token值, 进而获取写权限

目前put的效率是比较低的，默认情况下需要30s左右的时间，99%的时间消耗就在get nodes阶段；

我们解析下get nodes阶段的过程：

1. 从当前routing table中查找K个离target相近的节点，默认的K值等于routing table中K桶的大小，默认为8；

2. 从候选的K个节点中发出第一轮的add_requests请求，该过程设计到几个环节-参数：
    
    1) branch_factor(并行搜索alpha因子) - 单次add_requests执行中并行连接的个数，目前有两种模式

        aggressive_lookups: 

            outstanding < m_branch_factor  - outstanding是add_requests中的一个局部变量，get过程中会有多次执行add_requests，每次执行都会置0；

        not aggressive_lookups:

            m_invoke_count < m_branch_factor - m_invoke_count是一个全局变量，表示整个get过程中invoke发出的数量；

    2) results_target(token数量) && outstanding==0 && timeout

        单次add_requests执行时，会遍历当前的候选连接节点m_results，发出invoke请求，这里特指get请求；

        m_results中的节点成员是经过cidr距离排序的，但是存在多种状态，包括未发出get请求的，有发出get请求未返回的，也有发出get请求返回的

        代码中o->flags就代表了每个invoke请求的状态, 根据每个成员的状态，会有不同的响应：

            observer::flag_alive-代表发出的请求返回，所以results_target--，代表已成功返回，获取到write token

            observer::flag_queried-代表发出的请求未返回，所以outstanding++，代表还有连接在途中，未返回； 

            无状态就先赋值flag_queried，后发出invoke请求；


        我们再回到参数设置的解释上：

            results_target-需要多少个result-token返回

            outstanding == 0，是否还有invoke请求未返回

            在libtorrent原版代码中，这两个条件是且关系-(results_target == 0 && outstanding == 0)，代表了返回这么多token的前提下，还需要不存在in flight的请求，才能执行done状态。

            timeout - short, long timeout, 它决定了一次invoke多久会变为observer::flag_failed, 否则在long_timeout的范围内始终是observer::flag_queried状态，也就会存在outstanding > 0的情况；


3. 后续的过程是一个递归过程，前一步骤中发出的get请求，会返回一批邻近target的节点，再次发出get请求；

4. 在上述递归过程中，会根据是否终止条件发出get请求；结果的返回也会根据逻辑距离更新每一轮候选节点结果，终止条件对于put而言就只是在add_requests中返回；

5. 从排序好的候选结果中选取num_results个节点中进行put操作, put的个数是候选节点的个数和num_results两者的最小值, 真正执行成功的put个数会<=这个数字；


目前认为必要的加速策略：

1. 单层routing table，节点扩增到>=4096，这个策略可以保证每个target-(主要是target有限个)邻近节点的累积，不需要KAD的递归过程，简单高效, 它是token数量减小的基础；

再考虑策略:

results_target(token数量) && outstanding==0 && timeout

网络状况好的情况下，short timeout= 200ms, long timeout=500ms，就可以达到加速目的；

网络状况一般的情况下 1 <= results_target <= 2; remove outstanding == 0；
