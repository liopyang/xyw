<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  miniContentApi,
  type MiniArticle,
  type MiniCategory,
  type MiniPlace,
} from '../../api/miniContent';

type Block = {
  type: string;
  text?: string;
  label?: string;
  value?: string;
  mediaId?: number;
};
const route = useRoute();
const mode = computed(() => String(route.meta.miniMode || 'articles'));
const loading = ref(false);
const dialog = ref(false);

const categories = ref<MiniCategory[]>([]);
const articles = ref<MiniArticle[]>([]);
const places = ref<MiniPlace[]>([]);
const placeCategories = ref<any[]>([]);
const records = ref<any[]>([]);
const category = reactive<Partial<MiniCategory>>({ sortOrder: 0, status: 1 });
const article = reactive<Partial<MiniArticle>>({
  sortOrder: 0,
  contentBlocksJson: '[]',
});
const place = reactive<Partial<MiniPlace>>({
  sortOrder: 0,
  status: 1,
  detailBlocksJson: '[]',
});
const home = reactive({
  siteName: '校园业务服务',
  topDescription: '本页面提供信息整理和问题反馈服务。',
  notice: '具体套餐、办理结果和服务规则，以对应运营单位正式说明为准。',
  noticeEnabled: true,
  banners: [] as any[],
  cards: [] as any[],
});
const bannersText = ref('[]');
const cardsText = ref('[]');
const blocks = ref<Block[]>([]);
const blockTypes = [
  ['heading', '大标题'],
  ['subheading', '小标题'],
  ['paragraph', '正文'],
  ['notice', '重点提示'],
  ['image', '图片'],
  ['phone', '联系电话'],
  ['copy_link', '可复制链接'],
  ['copy_text', '可复制文字'],
  ['divider', '分隔线'],
];

function parseConfigValue(value: unknown) {
  return typeof value === 'string' ? JSON.parse(value) : value;
}

function applyHomeConfig(configKey: string, rawValue: unknown) {
  const value = parseConfigValue(rawValue);

  if (configKey === 'site') {
    Object.assign(home, value);
    return;
  }

  if (configKey === 'notice') {
    Object.assign(home, value);
    return;
  }

  if (configKey === 'banners') {
    home.banners = value || [];
    return;
  }

  if (configKey === 'cards') {
    home.cards = value || [];
  }
}

async function loadHomeConfig() {
  const rows = (await miniContentApi.home()).data;

  for (const row of rows) {
    applyHomeConfig(row.configKey, row.configValue);
  }

  bannersText.value = JSON.stringify(home.banners, null, 2);
  cardsText.value = JSON.stringify(home.cards, null, 2);
}

async function loadPlaces() {
  places.value = (await miniContentApi.places()).data;
  placeCategories.value = (await miniContentApi.placeCategories()).data;
}

async function loadCurrentSection() {
  if (mode.value === 'home') {
    await loadHomeConfig();
    return;
  }

  if (mode.value === 'articles') {
    articles.value = (await miniContentApi.articles()).data;
    return;
  }

  if (mode.value === 'places') {
    await loadPlaces();
    return;
  }

  if (mode.value === 'records') {
    records.value = (await miniContentApi.records()).data;
  }
}

async function load() {
  loading.value = true;

  try {
    categories.value = (await miniContentApi.categories()).data;
    await loadCurrentSection();
  } finally {
    loading.value = false;
  }
}

function openCategory(row?: MiniCategory) {
  Object.assign(
    category,
    {
      id: undefined,
      categoryCode: '',
      categoryName: '',
      description: '',
      sortOrder: 0,
      status: 1,
    },
    row || {},
  );
  dialog.value = true;
}

async function openArticle(row?: MiniArticle) {
  Object.assign(
    article,
    {
      id: undefined,
      categoryId: categories.value[0]?.id,
      title: '',
      subtitle: '',
      summary: '',
      sortOrder: 0,
      contentBlocksJson: '[]',
    },
    row || {},
  );
  blocks.value = [];

  if (row) {
    const response = await miniContentApi.article(row.id);
    Object.assign(article, response.data);
    blocks.value = JSON.parse(response.data.contentBlocksJson || '[]');
  }

  dialog.value = true;
}

function openPlace(row?: MiniPlace) {
  Object.assign(
    place,
    {
      id: undefined,
      categoryId: placeCategories.value[0]?.id,
      placeName: '',
      longitude: 0,
      latitude: 0,
      address: '',
      summary: '',
      contactPhone: '',
      businessHours: '',
      sortOrder: 0,
      status: 1,
      detailBlocksJson: '[]',
    },
    row || {},
  );
  blocks.value = JSON.parse(row?.detailBlocksJson || '[]');
  dialog.value = true;
}

function addBlock(type: string) {
  blocks.value.push({
    type,
    text: '',
    label: type === 'copy_link' ? '链接标题' : '',
    value: '',
  });
}

function move(index: number, offset: number) {
  const target = index + offset;

  if (target < 0 || target >= blocks.value.length) {
    return;
  }

  const item = blocks.value.splice(index, 1)[0];

  if (item) {
    blocks.value.splice(target, 0, item);
  }
}

async function saveCategory() {
  if (category.id) {
    await miniContentApi.updateCategory(category.id, category);
  } else {
    await miniContentApi.createCategory(category);
  }
}

async function saveArticle() {
  article.contentBlocksJson = JSON.stringify(blocks.value);

  if (article.id) {
    await miniContentApi.updateArticle(article.id, article);
  } else {
    await miniContentApi.createArticle(article);
  }
}

async function savePlace() {
  place.detailBlocksJson = JSON.stringify(blocks.value);

  if (place.id) {
    await miniContentApi.updatePlace(place.id, place);
  } else {
    await miniContentApi.createPlace(place);
  }
}

async function save() {
  if (mode.value === 'categories') {
    await saveCategory();
  } else if (mode.value === 'articles') {
    await saveArticle();
  } else if (mode.value === 'places') {
    await savePlace();
  }

  dialog.value = false;
  ElMessage.success('已保存');
  await load();
}

async function saveHome() {
  try {
    home.banners = JSON.parse(bannersText.value);
    home.cards = JSON.parse(cardsText.value);
  } catch {
    ElMessage.error('轮播或卡片 JSON 格式不正确');
    return;
  }

  await miniContentApi.saveHome({
    site: { siteName: home.siteName, topDescription: home.topDescription },
    notice: { notice: home.notice, noticeEnabled: home.noticeEnabled },
    banners: home.banners,
    cards: home.cards,
  });
  ElMessage.success('首页配置已保存');
}

async function publish(row: MiniArticle) {
  await miniContentApi.publish(row.id);
  ElMessage.success('已发布');
  await load();
}

async function offline(row: MiniArticle) {
  await miniContentApi.offline(row.id);
  ElMessage.success('已下线');
  await load();
}

async function remove(kind: 'category' | 'place', id: number) {
  await ElMessageBox.confirm('确定删除吗？', '提示', { type: 'warning' });

  if (kind === 'category') {
    await miniContentApi.deleteCategory(id);
  } else {
    await miniContentApi.deletePlace(id);
  }

  await load();
}

watch(mode, load);
onMounted(load);
</script>

<template>
  <div
    class="page"
    v-loading="loading"
  >
    <div class="heading">
      <div>
        <h1 class="page-title">{{ String(route.meta.title) }}</h1>
        <p class="page-desc">小程序展示内容由数据库动态发布，不需要重新提交小程序代码</p>
      </div>
      <el-button
        v-if="mode === 'categories'"
        type="primary"
        @click="openCategory()"
      >
        新增栏目
      </el-button>
      <el-button
        v-if="mode === 'articles'"
        type="primary"
        @click="openArticle()"
      >
        新增图文
      </el-button>
      <el-button
        v-if="mode === 'places'"
        type="primary"
        @click="openPlace()"
      >
        新增地点
      </el-button>
    </div>
    <div
      v-if="mode === 'home'"
      class="panel form"
    >
      <el-form label-width="120px">
        <el-form-item label="小程序名称"><el-input v-model="home.siteName" /></el-form-item>
        <el-form-item label="顶部说明">
          <el-input
            v-model="home.topDescription"
            type="textarea"
          />
        </el-form-item>
        <el-form-item label="通知启用"><el-switch v-model="home.noticeEnabled" /></el-form-item>
        <el-form-item label="通知文字">
          <el-input
            v-model="home.notice"
            type="textarea"
          />
        </el-form-item>
        <el-form-item label="轮播配置">
          <el-input
            v-model="bannersText"
            type="textarea"
            :rows="6"
            placeholder='[{"mediaId":1,"sortOrder":10,"enabled":true}]'
          />
        </el-form-item>
        <el-form-item label="卡片配置">
          <el-input
            v-model="cardsText"
            type="textarea"
            :rows="6"
            placeholder='[{"title":"校园卡","articleId":1,"sortOrder":10,"enabled":true}]'
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="saveHome"
          >
            保存首页配置
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <div
      v-else
      class="panel table-panel"
    >
      <el-table
        v-if="mode === 'categories'"
        :data="categories"
      >
        <el-table-column
          prop="categoryCode"
          label="编号"
        />
        <el-table-column
          prop="categoryName"
          label="栏目"
        />
        <el-table-column
          prop="description"
          label="说明"
        />
        <el-table-column
          prop="sortOrder"
          label="排序"
        />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button
              link
              @click="openCategory(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="remove('category', row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-table
        v-if="mode === 'articles'"
        :data="articles"
      >
        <el-table-column
          prop="articleNo"
          label="内容编号"
        />
        <el-table-column
          prop="categoryName"
          label="栏目"
        />
        <el-table-column
          prop="title"
          label="标题"
        />
        <el-table-column
          prop="publishStatus"
          label="状态"
        />
        <el-table-column
          label="操作"
          width="220"
        >
          <template #default="{ row }">
            <el-button
              link
              @click="openArticle(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="success"
              @click="publish(row)"
            >
              发布
            </el-button>
            <el-button
              link
              type="warning"
              @click="offline(row)"
            >
              下线
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-table
        v-if="mode === 'places'"
        :data="places"
      >
        <el-table-column
          prop="placeNo"
          label="地点编号"
        />
        <el-table-column
          prop="placeName"
          label="地点"
        />
        <el-table-column
          prop="categoryName"
          label="分类"
        />
        <el-table-column
          prop="address"
          label="地址"
        />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button
              link
              @click="openPlace(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="remove('place', row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-table
        v-if="mode === 'records'"
        :data="records"
      >
        <el-table-column
          prop="title"
          label="内容"
        />
        <el-table-column
          prop="versionNo"
          label="版本"
        />
        <el-table-column
          prop="operationType"
          label="操作"
        />
        <el-table-column
          prop="publishedAt"
          label="时间"
        />
      </el-table>
    </div>
    <el-dialog
      v-model="dialog"
      :title="
        mode === 'categories' ? '栏目编辑' : mode === 'places' ? '地点编辑' : '结构化图文编辑'
      "
      width="760px"
    >
      <el-form
        v-if="mode === 'categories'"
        label-width="90px"
      >
        <el-form-item label="栏目编号"><el-input v-model="category.categoryCode" /></el-form-item>
        <el-form-item label="栏目名称"><el-input v-model="category.categoryName" /></el-form-item>
        <el-form-item label="栏目说明"><el-input v-model="category.description" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="category.sortOrder" /></el-form-item>
      </el-form>
      <el-form
        v-else
        label-width="90px"
      >
        <template v-if="mode === 'articles'">
          <el-form-item label="栏目">
            <el-select v-model="article.categoryId">
              <el-option
                v-for="c in categories"
                :key="c.id"
                :label="c.categoryName"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标题"><el-input v-model="article.title" /></el-form-item>
          <el-form-item label="副标题"><el-input v-model="article.subtitle" /></el-form-item>
          <el-form-item label="摘要">
            <el-input
              v-model="article.summary"
              type="textarea"
            />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="分类">
            <el-select v-model="place.categoryId">
              <el-option
                v-for="c in placeCategories"
                :key="c.id"
                :label="c.categoryName"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="地点名称"><el-input v-model="place.placeName" /></el-form-item>
          <el-form-item label="经纬度">
            <el-input-number
              v-model="place.longitude"
              :precision="7"
            />
            <el-input-number
              v-model="place.latitude"
              :precision="7"
            />
          </el-form-item>
          <el-form-item label="地址"><el-input v-model="place.address" /></el-form-item>
          <el-form-item label="电话"><el-input v-model="place.contactPhone" /></el-form-item>
          <el-form-item label="营业时间"><el-input v-model="place.businessHours" /></el-form-item>
        </template>
        <el-divider>内容块</el-divider>
        <div class="block-tools">
          <el-button
            v-for="item in blockTypes"
            :key="item[0]"
            size="small"
            @click="addBlock(item[0]!)"
          >
            + {{ item[1] }}
          </el-button>
        </div>
        <div
          v-for="(block, index) in blocks"
          :key="index"
          class="block"
        >
          <b>{{ block.type }}</b>
          <el-input
            v-if="block.type !== 'divider' && block.type !== 'image'"
            v-model="block.text"
            placeholder="显示文字"
          />
          <el-input
            v-if="
              block.type === 'copy_link' || block.type === 'copy_text' || block.type === 'phone'
            "
            v-model="block.value"
            placeholder="要复制的值"
          />
          <el-input-number
            v-if="block.type === 'image'"
            v-model="block.mediaId"
            placeholder="素材 ID"
          />
          <el-button
            link
            @click="move(index, -1)"
          >
            上移
          </el-button>
          <el-button
            link
            @click="move(index, 1)"
          >
            下移
          </el-button>
          <el-button
            link
            type="danger"
            @click="blocks.splice(index, 1)"
          >
            删除
          </el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="save"
        >
          保存草稿
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<style scoped>
.heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.form {
  padding: 24px;
  max-width: 900px;
}
.block-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.block {
  display: grid;
  grid-template-columns: 90px 1fr 1fr auto auto auto;
  gap: 8px;
  align-items: center;
  padding: 10px;
  background: #f7f9fc;
  border-radius: 8px;
  margin: 8px 0;
}
</style>
