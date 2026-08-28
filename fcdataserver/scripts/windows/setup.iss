#ifndef AppImageDir
  #define AppImageDir "..\..\dist\staging\FC26Career"
#endif
#ifndef OutputDir
  #define OutputDir "..\..\dist"
#endif

#define MyAppName "FC26经理模式档案"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "FCData"
#define MyAppExeName "FC26Career.exe"

[Setup]
AppId={{8F3C1A2E-7B64-4E19-9D5C-FC26C0DE2026}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={localappdata}\Programs\FC26Career
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir={#OutputDir}
OutputBaseFilename=FC26Career-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
SetupIconFile=app.ico
UninstallDisplayIcon={app}\{#MyAppExeName}
UninstallDisplayName={#MyAppName}
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0
CloseApplications=yes
RestartApplications=no
UsedUserAreasWarning=no
AllowNoIcons=yes

[Languages]
Name: "chinesesimplified"; MessagesFile: "compiler:Default.isl"

[LangOptions]
chinesesimplified.LanguageName=简体中文
chinesesimplified.LanguageID=$0804
chinesesimplified.LanguageCodePage=936

[Messages]
SetupAppTitle=安装
SetupWindowTitle=安装 - %1
UninstallAppTitle=卸载
UninstallAppFullTitle=%1 卸载
InformationTitle=信息
ConfirmTitle=确认
ErrorTitle=错误
SetupAppRunningError=安装程序检测到 %1 当前正在运行。%n%n请先关闭该程序，然后点击“确定”继续，或点击“取消”退出。
UninstallAppRunningError=卸载程序检测到 %1 当前正在运行。%n%n请先关闭该程序，然后点击“确定”继续，或点击“取消”退出。
ExitSetupTitle=退出安装
ExitSetupMessage=安装尚未完成。如果现在退出，程序将不会被安装。%n%n确定退出吗？
ButtonBack=< 上一步(&B)
ButtonNext=下一步(&N) >
ButtonInstall=安装(&I)
ButtonOK=确定
ButtonCancel=取消
ButtonFinish=完成(&F)
ButtonBrowse=浏览(&B)...
ClickNext=点击“下一步”继续，或点击“取消”退出。
BrowseDialogTitle=浏览文件夹
BrowseDialogLabel=选择一个文件夹，然后点击“确定”。
NewFolderName=新建文件夹
WelcomeLabel1=欢迎使用 [name] 安装向导
WelcomeLabel2=即将在您的电脑上安装 [name/ver]。%n%n建议先关闭其他程序再继续。
WizardSelectDir=选择安装位置
SelectDirDesc=您想将 [name] 安装到哪里？
SelectDirLabel3=安装程序会把 [name] 安装到下面的文件夹。
SelectDirBrowseLabel=点击“下一步”继续。如需更换文件夹，请点击“浏览”。
DiskSpaceMBLabel=至少需要 [mb] MB 可用磁盘空间。
WizardSelectTasks=附加任务
SelectTasksDesc=还可以执行以下任务
SelectTasksLabel2=请选择安装 [name] 时要执行的附加任务，然后点击“下一步”。
WizardReady=准备安装
ReadyLabel1=已经准备好将 [name] 安装到这台电脑。
ReadyLabel2a=点击“安装”开始。若要修改设置，请点击“上一步”。
ReadyLabel2b=点击“安装”开始。
ReadyMemoDir=安装位置：
ReadyMemoGroup=开始菜单文件夹：
ReadyMemoTasks=附加任务：
WizardPreparing=正在准备安装
PreparingDesc=正在准备将 [name] 安装到这台电脑。
WizardInstalling=正在安装
InstallingLabel=正在安装 [name]，请稍候。
FinishedHeadingLabel=完成 [name] 安装向导
FinishedLabelNoIcons=已经在这台电脑上安装了 [name]。
FinishedLabel=已经在这台电脑上安装了 [name]。可以通过快捷方式打开它。
ClickFinish=点击“完成”退出安装程序。
StatusCreateDirs=正在创建目录...
StatusExtractFiles=正在解压文件...
StatusCreateIcons=正在创建快捷方式...
StatusSavingUninstall=正在保存卸载信息...
StatusRunProgram=正在完成安装...
ConfirmUninstall=确定要完全移除 %1 吗？
UninstallStatusLabel=正在从电脑中移除 %1，请稍候。
UninstalledAll=已从电脑中移除 %1。
WizardUninstalling=卸载状态
StatusUninstalling=正在卸载 %1...

[CustomMessages]
CreateDesktopIcon=创建桌面快捷方式(&D)
AdditionalIcons=附加快捷方式：
UninstallProgram=卸载 %1
LaunchProgram=立即运行 %1

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checkedonce

[Files]
Source: "{#AppImageDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Tasks: desktopicon
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#MyAppName}}"; Flags: nowait postinstall skipifsilent
