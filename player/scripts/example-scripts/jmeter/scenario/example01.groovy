def srcs = ctx.getSources();

srcs.each({
    System.out.println("replace " + it + " with " + ctx.getTemplate(it).toString());
})




















