<template>
  <a-card :bordered="false">
    <!-- 查询区域 -->
    <div class="table-page-search-wrapper">
      <a-form layout="inline" @keyup.enter.native="searchQuery">
        <a-row :gutter="24">
          <a-col :md="4" :sm="24">
            <a-form-item label="仓库">
              <a-select
                style="width: 100%"
                placeholder="请选择仓库"
                v-model="queryParam.depotId">
                <a-select-option v-for="(depot,index) in depotList" :value="depot.id">
                  {{ depot.depotName }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="8">
            <a-form-item label="商品信息">
              <a-input placeholder="请输入商品名称、规格、型号" v-model="queryParam.materialParam"></a-input>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24" >
            <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
              <a-button type="primary" @click="searchQuery">查询</a-button>
              <a-button style="margin-left: 8px" type="primary" icon="download" @click="handleExportXls('库存预警')">导出</a-button>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>
    <!-- table区域-begin -->
    <a-table
      bordered
      ref="table"
      size="middle"
      rowKey="id"
      :columns="columns"
      :dataSource="dataSource"
      :pagination="ipagination"
      :loading="loading"
      @change="handleTableChange">
    </a-table>
    <!-- table区域-end -->
  </a-card>
</template>
<script>
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import {getAction} from '@/api/manage'
  import { getMpListShort } from "@/utils/util"
  import Vue from 'vue'
  export default {
    name: "BuyInReport",
    mixins:[JeecgListMixin],
    components: {
      JEllipsis
    },
    data () {
      return {
        // 查询条件
        queryParam: {
          materialParam:'',
          depotId: '',
          mpList: getMpListShort(Vue.ls.get('materialPropertyList'))  //扩展属性
        },
        depotList: [],
        tabKey: "1",
        // 表头
        columns: [
          {
            title: '#',
            dataIndex: '',
            key:'rowIndex',
            width:40,
            align:"center",
            customRender:function (t,r,index) {
              return parseInt(index)+1;
            }
          },
          {title: '名称', dataIndex: 'mname', width: 160},
          {title: '规格', dataIndex: 'mstandard', width: 80},
          {title: '型号', dataIndex: 'mmodel', width: 80},
          {title: '扩展信息', dataIndex: 'materialOther', width: 150},
          {title: '单位', dataIndex: 'materialUnit', width: 80},
          {title: '安全存量', dataIndex: 'safetystock', width: 80},
          {title: '当前库存', dataIndex: 'currentNumber', width: 80},
          {title: '建议入库量', dataIndex: 'linjieNumber', width: 80}
        ],
        labelCol: {
          xs: { span: 1 },
          sm: { span: 2 },
        },
        wrapperCol: {
          xs: { span: 10 },
          sm: { span: 16 },
        },
        url: {
          list: "/depotItem/findStockWarningCount",
          exportXlsUrl: "/depotItem/exportWarningExcel",
        }
      }
    },
    created () {
      this.getDepotData()
    },
    methods: {
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter);
        param.field = this.getQueryField();
        param.currentPage = this.ipagination.current;
        param.pageSize = this.ipagination.pageSize;
        return param;
      },
      getDepotData() {
        getAction('/depot/findDepotByCurrentUser').then((res)=>{
          if(res.code === 200){
            this.depotList = res.data;
          }else{
            this.$message.info(res.data);
          }
        })
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>