const router = require('koa-router')()
const configmap = require('../db')

router.get('/', async (ctx, next) => {
  await ctx.render('index', {
    title: 'Hello Koa 2!'
  })
})

router.get('/string', async (ctx, next) => {
  ctx.body = 'koa2 string'
})

router.get('/api', async (ctx, next) => {
  ctx.body = {
    name: configmap.nickname
  }
})

module.exports = router
