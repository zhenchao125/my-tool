package my

import java.io.{BufferedWriter, File, FileWriter}

import scala.io.Source

/**
  * Author lzc
  * Date 2019/5/18 11:34 AM
  */
object MultiMd2OneMd {
    var rootPath: String = "/Users/lzc/Desktop/gitbook/zhenchao125/bigdata_scala_atguigu"
    var project: String = _
    var baseUrl: String = _
    val summaryMd = "SUMMARY.md"
    
    var finalPath: String = "/Users/lzc/Desktop/mk转换过来的word"
    var finalMdFilePath: String = _
    var finalWordFilePath: String = _
    var windowPath = "/Volumes/[C] Windows 7.hidden/Users/lzc/Desktop/自动生成的文档"
    
    var codeCount: Int = _
    
    /**
      * 需要传入两个参数: 项目路径, 输出的文件的路径
      *
      * @param args
      */
    def main(args: Array[String]): Unit = {
        if (args.length < 1) throw new IllegalArgumentException("参数的个数不对, 至少需要提供输入的项目")
        
        rootPath = args(0)
        project = new File(rootPath).getName
        baseUrl = s"$rootPath"
        
        if (args.length == 2) finalPath = args(1)
        
        finalMdFilePath = s"$finalPath/$project.md"
        finalWordFilePath = s"$finalPath/$project.docx"
//        windowPath = s"$windowPath/$project.docx"
        
        val mdFiles: List[(Int, File)] = summary2List(s"$baseUrl/$summaryMd")
        
        list2OneMd(mdFiles, finalMdFilePath)
        
        md2Word
    }
    
    /**
      * md 转换成 word 文件
      */
    def md2Word() = {
        println(s"开始把 ${new File(finalMdFilePath).getName} 转换成 word 文档, 请耐心等待...")
        import scala.sys.process._
        val cmd = s"/usr/local/bin/pandoc -f markdown -t docx $finalMdFilePath " +
            s" --reference-doc=/Users/lzc/custom-reference.docx " +
            s" --highlight-style /Users/lzc/my.theme  " +
            s" -o $finalWordFilePath"
        cmd !;
        println(s"恭喜 ${new File(finalWordFilePath).getName} 转换成功!!!")
        println(s"文件路径所在: ${new File(finalWordFilePath).getAbsolutePath}")
        s"rm -rf $finalMdFilePath" !;
        println(s"删除 ${new File(finalMdFilePath).getName} 成功!")
        
        // copy 到 window
//        val copy2WindowCmd = s"cp $finalWordFilePath '$windowPath'"
//        copy2WindowCmd !;
//        println(s"copy 到 window 成功, 目录: $windowPath")
    }
    
    /**
      * 把 List 中的所有文件的内容拼接成一个 MD 文件
      * (2,/Users/lzc/Desktop/gitbook/zhenchao125/bigdata_scala_atguigu/11-wei-shi-yao-yao-xue-xi-scala.md)
      *
      * @param mdFiles
      */
    def list2OneMd(mdFiles: List[(Int, File)], finalFilePath: String) = {
        
        println(s"开始拼接 ${new File(finalFilePath).getName} 文件")
        val writer = new BufferedWriter(new FileWriter(finalFilePath))
        codeCount = 0
        mdFiles.foreach {
            case (level, mdFile) =>
                // md 文件的行
                val lines: Iterator[String] = Source.fromFile(mdFile).getLines()
                // 对标题行添加 #
                val stars: String = (1 to level / 2).foldLeft("")((zero, _) => zero + "#")
                lines.foreach(line => {
                    val finalLine: String = line match {
                        // 如果有水印图片, 则删除水印
                        case line if line.contains("-atguiguText") =>
                            "\n\n" + line.replaceAll("-atguiguText", "") + "\n\n"
                        // 如果 这行以 # 开头, 并且没有在代码内, 则在这行添加对应的 # 表示标题级别
                        case line if line.trim.startsWith("#") && !isInCode =>
                            stars + line.trim
                        // 如果以 ``` 开头, 表示代码开始或者结束部分 变量增 1
                        case line if line.trim.startsWith("```") =>
                            codeCount += 1
                            if (isInCode) "\n" + line
                            else line
                        // 如果是文件内行分隔符 --- , 则去掉这个分隔符
                        case line if line.trim == "---" =>
                            "\n"
                        // 更新 ppt 播放地址
                        case line if line.contains("/ppt/") =>
                            val index: Int = line.indexOf("/ppt/")
                            val ppt = line.substring(index)
                            s"[查看 ppt](http://www.zhenchao.cf$ppt"
                        // 其他任何情况原封不动返回
                        case line =>
                            line
                    }
                    writer.write(finalLine)
                    writer.newLine()
                })
                writer.newLine()
                writer.newLine()
                writer.write("---")
                writer.newLine()
                writer.newLine()
        }
        writer.close()
        println(s" ${new File(finalFilePath).getName} 文件拼接完成")
        
    }
    
    /**
      * 根据每个 gitbook 书籍的 summary 文档中的目录, 做出来一个需要拼接的 MD 文件的列表
      *
      * @param sumarryFile
      * @return
      */
    def summary2List(summaryFilePath: String): List[(Int, File)] = {
        println(s"开始读取 ${summaryMd} 文件")
        val result: List[(Int, File)] = Source.fromFile(summaryFilePath).getLines().toList.filter(line => {
            line.trim.length > 0 && line.contains("*") && line.contains("](")
        }).map(line => {
            val split: Array[String] = line.split("\\*")
            (split(0).length, split(1).split("\\]\\(|md\\)")(1) + "md")
        }).map {
            case (level, fileName) => (level, new File(s"$baseUrl/$fileName"))
        }
        println(s" ${summaryMd} 文件读取结束")
        result
    }
    
    /**
      * 是否在代码内部
      *
      * @return
      */
    def isInCode: Boolean = codeCount % 2 == 1
}
