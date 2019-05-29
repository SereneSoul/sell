<html>
<#include "../common/header.ftl">
<body>
<div id="wrapper" class="toggled">
  <#include "../common/nav.ftl">
  <div id="page-content-wrapper">
    <div class="container-fluid">
      <div class="row clearfix">
        <div class="col-md-12 column">
          <form role="form" method="post" action="/sell/seller/category/save">
            <div class="form-group">
              <label for="categoryName">名称</label>
              <input name="categoryName" type="text" class="form-control" id="categoryName" value="${(category.categoryName)!''}"/>
            </div>
            <div class="form-group">
              <label for="categoryType">类型</label>
              <input name="categoryType" type="text" class="form-control" id="categoryType" value="${(category.categoryType)!''}"/>
            </div>
            <input hidden type="text" name="categoryId" value="${(category.categoryId)!''}">
            <button type="submit" class="btn btn-default">保存</button>
          </form>
        </div>
      </div>
    </div>
  </div>
</div>
</body>
</html>