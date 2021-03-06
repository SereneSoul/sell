<html>
  <#include "../common/header.ftl">
  <body>
    <div id="wrapper" class="toggled">
      <#include "../common/nav.ftl">
      <div id="page-content-wrapper">
        <div class="container-fluid">
          <div class="row clearfix">
            <div class="col-md-12 column">
              <table class="table table-striped table-hover table-condensed">
                <thead>
                <tr>
                  <th>商品编号</th>
                  <th>名称</th>
                  <th>图片</th>
                  <th>库存</th>
                  <th>描述</th>
                  <th>类目</th>
                  <th>创建时间</th>
                  <th>修改时间</th>
                  <th colspan="2">操作</th>
                </tr>
                </thead>
                <tbody>
                <#list productInfoPage.content as product>
                  <tr>
                    <td>${product.productId}</td>
                    <td>${product.productName}</td>
                    <td><img height="50" width="50" src="${product.productIcon}"></td>
                    <td>${product.productStock}</td>
                    <td>${product.productDescription}</td>
                    <td>${product.categoryType}</td>
                    <td>${product.createTime}</td>
                    <td>${product.updateTime}</td>
                    <td><a href="/sell/seller/product/index?productId=${product.productId}" type="button" class="btn btn-sm btn-danger">修改</a></td>
                    <td>
                      <#if product.productStatus == 0>
                        <a href="/sell/seller/product/off_sale?productId=${product.productId}" type="button" class="btn btn-sm btn-warning">下架</a>
                      <#else>
                        <a href="/sell/seller/product/on_sale?productId=${product.productId}" type="button" class="btn btn-sm btn-info">上架</a>
                      </#if>
                    </td>
                  </tr>
                </#list>
                </tbody>
              </table>
            </div>
            <div class="col-md-12 column">
              <ul class="pagination pull-right">
                <#if productInfoPage.getPageable().getPageNumber() + 1 lte 1>
                  <li class="disabled">
                    <a href=#">上一页</a>
                  </li>
                <#else>
                  <li>
                    <a href="/sell/seller/product/list?page=${productInfoPage.getPageable().getPageNumber() - 1}">上一页</a>
                  </li>
                </#if>

                <#list 1..productInfoPage.getTotalPages() as index>
                  <#if productInfoPage.getPageable().getPageNumber() + 1 == index>
                    <li class="disabled">
                      <a href="/sell/seller/product/list?page=${index - 1}">${index}</a>
                    </li>
                  <#else>
                    <li>
                      <a href="/sell/seller/product/list?page=${index - 1}">${index}</a>
                    </li>
                  </#if>
                </#list>
                <#if productInfoPage.getPageable().getPageNumber() + 1 gte productInfoPage.getTotalPages()>
                  <li class="disabled">
                    <a href="#">下一页</a>
                  </li>
                <#else>
                  <li>
                    <a href="/sell/seller/product/list?page=${productInfoPage.getPageable().getPageNumber() + 1}">下一页</a>
                  </li>
                </#if>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>